package com.game.controller;

import com.game.domain.Card;
import com.game.domain.Phase;
import com.game.engine.GameContext;
import com.game.engine.GameStateMachine;
import com.game.event.GameEventPublisher;
import com.game.event.PlayerDamageEvent;
import com.game.event.PlayerPlayCardEvent;
import com.game.card.adapter.PlayerCardTargetAdapter;
import com.game.service.BotService;
import com.game.service.CardPlayService;
import com.game.service.RoomService;
import com.game.skill.GeneralRegistry;
import com.game.skill.skills.LongdanSkill;
import com.game.websocket.GameMessage;
import com.game.websocket.GameWebSocketHandler;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 游戏 HTTP 接口：开局（仅房主）、出牌（杀/桃/闪）、结束回合；所有状态变更后广播 GameContext。
 */
@RestController
@RequestMapping("/api/game")
public class GameController {

    private final RoomService roomService;
    private final GameEventPublisher eventPublisher;
    private final GameWebSocketHandler webSocketHandler;
    private final com.game.engine.DefaultGameStarter defaultGameStarter;
    private final BotService botService;
    private final com.game.service.CardPlayService cardPlayService;

    public GameController(RoomService roomService,
                          GameEventPublisher eventPublisher,
                          GameWebSocketHandler webSocketHandler,
                          com.game.engine.DefaultGameStarter defaultGameStarter,
                          BotService botService,
                          com.game.service.CardPlayService cardPlayService) {
        this.roomService = roomService;
        this.eventPublisher = eventPublisher;
        this.webSocketHandler = webSocketHandler;
        this.defaultGameStarter = defaultGameStarter;
        this.botService = botService;
        this.cardPlayService = cardPlayService;
    }

    /**
     * 可选武将列表（用于选将框）。
     */
    @GetMapping(value = "/generals", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<Map<String, Object>>> listGenerals() {
        return Mono.fromCallable(() ->
                GeneralRegistry.listSelectable().stream()
                        .map(g -> Map.<String, Object>of(
                                "id", g.getId(),
                                "name", g.getName(),
                                "skills", g.getSkills().stream()
                                        .map(s -> Map.of("id", s.getId(), "name", s.getName(), "description", s.getDescription() != null ? s.getDescription() : ""))
                                        .toList()))
                        .toList());
    }

    /**
     * 开局：仅房主可触发。初始化牌堆（52 张杀/闪/桃）、发 4 张、当前玩家摸 2 张进入 PLAY 阶段，并全量推送。
     * body 可含 generalId：房主选择的武将（选将框）。
     */
    @PostMapping(value = "/{roomId}/start", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> startGame(@PathVariable String roomId,
                                               @RequestBody(required = false) Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            String playerId = body != null && body.get("playerId") != null
                    ? body.get("playerId").toString() : null;
            String ownerGeneralId = body != null && body.get("generalId") != null
                    ? body.get("generalId").toString() : "zhaoyun";
            var resultOpt = roomService.startGame(roomId, playerId, defaultGameStarter, ownerGeneralId);
            if (resultOpt.isEmpty()) {
                return Map.<String, Object>of("ok", false, "message",
                        playerId != null && roomService.getRoom(roomId).isPresent()
                                && !playerId.equals(roomService.getRoom(roomId).get().getOwnerId())
                                ? "仅房主可以开始游戏" : "开局失败");
            }
            RoomService.StartResult result = resultOpt.get();
            GameContext ctx = result.context();
            webSocketHandler.broadcastGameContext(roomId, ctx);
            // WebSocket 下发手牌（完整实现）；HTTP 也返回当前玩家手牌，方便前端直接展示（MVP）
            for (var p : ctx.getRoom().getPlayers()) {
                webSocketHandler.sendHandToPlayer(roomId, p.getPlayerId(), List.copyOf(p.getHandCards()));
            }
            List<Card> drawn = result.drawnCardsForCurrentPlayer();
            if (!drawn.isEmpty()) {
                ctx.getCurrentPlayer().ifPresent(cur ->
                        webSocketHandler.sendDrawnCards(roomId, cur.getPlayerId(), drawn));
            }
            return buildGameStateResponse(ctx, roomId);
        });
    }

    /**
     * 获取当前游戏状态（用于页面重新可见时同步，解决后台标签页 WebSocket 消息被节流的问题）。
     */
    @GetMapping(value = "/{roomId}/state", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getGameState(@PathVariable String roomId,
                                                   @RequestParam(required = false) String playerId) {
        return Mono.fromCallable(() -> {
            var ctxOpt = roomService.getOrCreateContext(roomId);
            if (ctxOpt.isEmpty()) {
                return Map.<String, Object>of("ok", false, "message", "对局未开始");
            }
            return buildGameStateResponse(ctxOpt.get(), roomId);
        });
    }

    /**
     * 出牌：PLAY 阶段有效。杀→待目标响应（出闪抵消或承受伤害）；桃→回血；闪→仅在被杀时可出，用于抵消。
     */
    @PostMapping(value = "/{roomId}/play", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> playCard(@PathVariable String roomId,
                                              @RequestBody Map<String, String> body) {
        return Mono.fromCallable(() -> {
            var ctxOpt = roomService.getOrCreateContext(roomId);
            var smOpt = roomService.getStateMachine(roomId);
            if (ctxOpt.isEmpty() || smOpt.isEmpty()) {
                return Map.<String, Object>of("ok", false, "message", "对局未开始");
            }
            // 若有待响应的杀，允许目标出闪（不要求是回合玩家）
            if (ctxOpt.get().getPendingKill().isPresent()) {
                return handleRespondWithShan(roomId, body, ctxOpt.get());
            }
            if (!smOpt.get().canPlayCard()) {
                return Map.<String, Object>of("ok", false, "message", "当前不能出牌");
            }
            String playerId = body.get("playerId");
            String cardId = body.get("cardId");
            String targetId = body.get("targetId");
            if (playerId == null || cardId == null) {
                return Map.<String, Object>of("ok", false, "message", "缺少 playerId 或 cardId");
            }
            GameContext ctx = ctxOpt.get();
            GameStateMachine sm = smOpt.get();
            if (ctx.getCurrentPlayer().map(p -> !p.getPlayerId().equals(playerId)).orElse(true)) {
                return Map.<String, Object>of("ok", false, "message", "当前不是你的回合");
            }
            var playerOpt = ctx.getRoom().getPlayerById(playerId);
            if (playerOpt.isEmpty()) {
                return Map.<String, Object>of("ok", false, "message", "玩家不存在");
            }
            var player = playerOpt.get();
            var cardOpt = player.getHandCards().stream().filter(c -> c.getId().equals(cardId)).findFirst();
            if (cardOpt.isEmpty()) {
                return Map.<String, Object>of("ok", false, "message", "手牌中无此牌");
            }
            Card card = cardOpt.get();
            String type = card.getRankOrName();
            if ("闪".equals(type)) {
                return Map.<String, Object>of("ok", false, "message", "闪只能在被杀时响应使用");
            }
            if ("杀".equals(type)) {
                return handlePlaySha(roomId, ctx, sm, player, card, targetId);
            }
            if ("桃".equals(type)) {
                // 桃只能给自己使用，且满血时不能使用
                if (targetId != null && !targetId.isBlank() && !targetId.equals(playerId)) {
                    return Map.<String, Object>of("ok", false, "message", "桃只能给自己使用");
                }
                var targetOpt = ctx.getRoom().getPlayerById(playerId);
                if (targetOpt.isEmpty() || !targetOpt.get().isAlive()) {
                    return Map.<String, Object>of("ok", false, "message", "目标无效");
                }
                var target = targetOpt.get();
                if (target.getHp() >= target.getMaxHp()) {
                    return Map.<String, Object>of("ok", false, "message", "满血时不能使用桃");
                }
                player.removeHandCard(card);
                ctx.addToDiscardPile(card);
                int before = target.getHp();
                target.setHp(Math.min(target.getMaxHp(), before + 1));
                eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card));
                ctx.addPlayedCard(playerId, cardId, type, playerId, player.getNickname());
            } else if ("酒".equals(type)) {
                // 酒只能对自己使用；每回合只能出1次；出牌阶段仅用于 buff（下一张杀+1），回血仅在濒死时
                if (targetId != null && !targetId.isBlank() && !targetId.equals(playerId)) {
                    return Map.<String, Object>of("ok", false, "message", "酒只能对自己使用");
                }
                int jiuUsed = ctx.getAttribute("jiuUsedThisTurn").map(o -> ((Number) o).intValue()).orElse(0);
                if (jiuUsed >= 1) {
                    return Map.<String, Object>of("ok", false, "message", "本回合已使用过酒");
                }
                player.removeHandCard(card);
                ctx.addToDiscardPile(card);
                ctx.setAttribute("jiuUsedThisTurn", 1);
                ctx.setAttribute("jiuBuff", true); // 下一张杀伤害+1
                eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card));
                ctx.addPlayedCard(playerId, cardId, type, playerId, player.getNickname());
            } else if (CardPlayService.isHandledByCardSystem(type)) {
                if (CardPlayService.isEquipment(type)) {
                    targetId = playerId;
                } else if (CardPlayService.trickNeedsTarget(type)) {
                    if (targetId == null || targetId.isBlank()) {
                        return Map.<String, Object>of("ok", false, "message", "此锦囊需要指定目标");
                    }
                    var targetOpt = ctx.getRoom().getPlayerById(targetId);
                    if (targetOpt.isEmpty() || !targetOpt.get().isAlive()) {
                        return Map.<String, Object>of("ok", false, "message", "目标无效");
                    }
                }
                player.removeHandCard(card);
                ctx.addToDiscardPile(card);
                var targetPlayer = (targetId != null && !targetId.isBlank())
                        ? ctx.getRoom().getPlayerById(targetId).orElse(null) : null;
                cardPlayService.executeCardEffect(ctx, player, card, targetPlayer);
                eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card));
                String tid = targetPlayer != null ? targetPlayer.getPlayerId() : null;
                String tname = targetPlayer != null ? targetPlayer.getNickname() : null;
                ctx.addPlayedCard(playerId, cardId, type, tid, tname);
            } else {
                return Map.<String, Object>of("ok", false, "message", "未知牌型: " + type);
            }
            webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("PLAY_CARD",
                    Map.of("playerId", playerId, "cardId", cardId, "cardType", type)));
            webSocketHandler.broadcastGameContext(roomId, ctx);
            return buildGameStateResponse(ctx, roomId);
        });
    }

    private Map<String, Object> handlePlaySha(String roomId, GameContext ctx, GameStateMachine sm,
                                             com.game.domain.Player player, Card card, String targetId) {
        // 每回合出杀次数限制：1 次，诸葛连弩无限
        int shaCount = ctx.getAttribute("shaCountThisTurn").map(o -> ((Number) o).intValue()).orElse(0);
        if (shaCount >= 1 && !com.game.card.adapter.PlayerCardTargetAdapter.hasZhuGeLianNu(player.getPlayerId())) {
            return Map.<String, Object>of("ok", false, "message", "本回合已出过杀，装备诸葛连弩可无限出杀");
        }
        String realTargetId = (targetId == null || targetId.isBlank()) ? player.getPlayerId() : targetId;
        var targetOpt = ctx.getRoom().getPlayerById(realTargetId);
        if (targetOpt.isEmpty() || !targetOpt.get().isAlive()) {
            return Map.<String, Object>of("ok", false, "message", "目标无效");
        }
        var target = targetOpt.get();
        int damageAmount = Boolean.TRUE.equals(ctx.getAttribute("jiuBuff").map(o -> (Boolean) o).orElse(false)) ? 2 : 1;
        if (damageAmount == 2) {
            ctx.setAttribute("jiuBuff", false); // 使用后清除
        }
        player.removeHandCard(card);
        ctx.addToDiscardPile(card);
        eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card, target));
        ctx.addPlayedCard(player.getPlayerId(), card.getId(), "杀", target.getPlayerId(), target.getNickname());
        webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("PLAY_CARD",
                Map.of("playerId", player.getPlayerId(), "cardId", card.getId(), "cardType", "杀",
                        "targetId", target.getPlayerId(), "targetName", target.getNickname())));
        ctx.setAttribute("shaCountThisTurn", shaCount + 1);
        ctx.setPendingKill(target.getPlayerId(), player.getPlayerId(), player.getNickname(), target.getNickname(), damageAmount);
        webSocketHandler.broadcastGameContext(roomId, ctx);
        if (BotService.isBot(target.getPlayerId())) {
            botService.respondToKill(roomId, ctx);
            webSocketHandler.broadcastGameContext(roomId, ctx);
        }
        return buildGameStateResponse(ctx, roomId);
    }

    private Map<String, Object> handleRespondWithShan(String roomId, Map<String, String> body, GameContext ctx) {
        var pendingOpt = ctx.getPendingKill();
        if (pendingOpt.isEmpty()) {
            return Map.<String, Object>of("ok", false, "message", "无待响应的杀");
        }
        var pending = pendingOpt.get();
        String targetId = (String) pending.get("targetId");
        String playerId = body.get("playerId");
        if (playerId == null || !playerId.equals(targetId)) {
            return Map.<String, Object>of("ok", false, "message", "仅被杀目标可出闪响应");
        }
        String cardId = body.get("cardId");
        if (cardId == null || cardId.isBlank()) {
            return Map.<String, Object>of("ok", false, "message", "请选择要出的闪");
        }
        var targetOpt = ctx.getRoom().getPlayerById(targetId);
        if (targetOpt.isEmpty()) {
            return Map.<String, Object>of("ok", false, "message", "目标不存在");
        }
        var target = targetOpt.get();
        var cardOpt = target.getHandCards().stream().filter(c -> c.getId().equals(cardId)).findFirst();
        if (cardOpt.isEmpty()) {
            return Map.<String, Object>of("ok", false, "message", "手牌中无此牌");
        }
        Card card = cardOpt.get();
        String skillId = body.get("skillId");
        if ("longdan".equals(skillId)) {
            if (!"杀".equals(card.getRankOrName())) {
                return Map.<String, Object>of("ok", false, "message", "龙胆技能只能将杀当闪使用");
            }
            var general = GeneralRegistry.get(target.getGeneralId()).orElse(null);
            if (general == null || general.getSkillById("longdan") == null) {
                return Map.<String, Object>of("ok", false, "message", "你不具有龙胆技能");
            }
            if (LongdanSkill.respondWithShaAsShan(roomId, ctx, target, card, pending,
                    eventPublisher, webSocketHandler)) {
                roomService.getStateMachine(roomId).ifPresent(sm -> {
                    botService.runBotTurnsUntilHuman(roomId, ctx, sm);
                    webSocketHandler.broadcastGameContext(roomId, ctx);
                });
                return buildGameStateResponse(ctx, roomId);
            }
        }
        if (!"闪".equals(card.getRankOrName())) {
            return Map.<String, Object>of("ok", false, "message", "只能出闪进行抵消");
        }
        target.removeHandCard(card);
        ctx.addToDiscardPile(card);
        ctx.clearPendingKill();
        ctx.addPlayedCard(targetId, cardId, "闪", null, null);
        eventPublisher.publish(new PlayerPlayCardEvent(ctx, target, card));
        webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("PLAY_CARD",
                Map.of("playerId", targetId, "cardId", cardId, "cardType", "闪")));
        webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("SHAN_NEGATED",
                Map.of("targetId", targetId, "sourceId", pending.get("sourceId"),
                        "sourceName", pending.get("sourceName"), "message", "出闪抵消了杀")));
        webSocketHandler.broadcastGameContext(roomId, ctx);
        // 出闪抵消后，杀被化解，当前仍是出杀机器人的回合，需驱动其继续出牌或结束回合
        roomService.getStateMachine(roomId).ifPresent(sm -> {
            botService.runBotTurnsUntilHuman(roomId, ctx, sm);
            webSocketHandler.broadcastGameContext(roomId, ctx);
        });
        return buildGameStateResponse(ctx, roomId);
    }

    /** 被杀目标选择承受伤害（不出闪） */
    @PostMapping(value = "/{roomId}/respond", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> respondToKill(@PathVariable String roomId,
                                                   @RequestBody Map<String, String> body) {
        return Mono.fromCallable(() -> {
            var ctxOpt = roomService.getOrCreateContext(roomId);
            if (ctxOpt.isEmpty()) {
                return Map.<String, Object>of("ok", false, "message", "对局未开始");
            }
            var pendingOpt = ctxOpt.get().getPendingKill();
            if (pendingOpt.isEmpty()) {
                return Map.<String, Object>of("ok", false, "message", "无待响应的杀");
            }
            var pending = pendingOpt.get();
            String targetId = (String) pending.get("targetId");
            String playerId = body != null ? body.get("playerId") : null;
            if (playerId == null || !playerId.equals(targetId)) {
                return Map.<String, Object>of("ok", false, "message", "仅被杀目标可响应");
            }
            String action = body.get("action");
            if ("PASS".equals(action) || action == null || action.isBlank()) {
                GameContext ctx = ctxOpt.get();
                var targetOpt = ctx.getRoom().getPlayerById(targetId);
                var sourceOpt = ctx.getRoom().getPlayerById((String) pending.get("sourceId"));
                int amount = ((Number) pending.getOrDefault("amount", 1)).intValue();
                if (targetOpt.isPresent() && sourceOpt.isPresent()) {
                    eventPublisher.publish(new PlayerDamageEvent(ctx, targetOpt.get(), sourceOpt.get(), amount));
                }
                webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("DAMAGE",
                        Map.of("targetId", targetId, "targetName", pending.get("targetName"),
                                "sourceId", pending.get("sourceId"), "sourceName", pending.get("sourceName"), "amount", amount)));
                ctx.clearPendingKill();
                webSocketHandler.broadcastGameContext(roomId, ctx);
                // 若进入濒死，先进行濒死轮询
                if (ctx.getPendingDeath().isPresent()) {
                    botService.runDyingPoll(roomId, ctx);
                    webSocketHandler.broadcastGameContext(roomId, ctx);
                    if (ctx.getPendingDeath().isEmpty()) {
                        var smOpt = roomService.getStateMachine(roomId);
                        if (smOpt.isPresent()) {
                            botService.runBotTurnsUntilHuman(roomId, ctx, smOpt.get());
                            webSocketHandler.broadcastGameContext(roomId, ctx);
                        }
                    }
                } else {
                    var smOpt = roomService.getStateMachine(roomId);
                    if (smOpt.isPresent()) {
                        botService.runBotTurnsUntilHuman(roomId, ctx, smOpt.get());
                        webSocketHandler.broadcastGameContext(roomId, ctx);
                    }
                }
                return buildGameStateResponse(ctx, roomId);
            }
            return Map.<String, Object>of("ok", false, "message", "无效的响应");
        });
    }

    /** 濒死轮询时人类响应：出桃救人或跳过 */
    @PostMapping(value = "/{roomId}/respondDying", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> respondDying(@PathVariable String roomId,
                                                 @RequestBody Map<String, String> body) {
        return Mono.fromCallable(() -> {
            var ctxOpt = roomService.getOrCreateContext(roomId);
            if (ctxOpt.isEmpty()) {
                return Map.<String, Object>of("ok", false, "message", "对局未开始");
            }
            var pendingOpt = ctxOpt.get().getPendingDeath();
            if (pendingOpt.isEmpty()) {
                return Map.<String, Object>of("ok", false, "message", "无人濒死");
            }
            var pending = pendingOpt.get();
            String targetId = (String) pending.get("targetId");
            String targetName = (String) pending.get("targetName");
            int askingSeat = ((Number) pending.get("askingSeatIndex")).intValue();
            String playerId = body != null ? body.get("playerId") : null;
            var askingOpt = ctxOpt.get().getRoom().getPlayerBySeat(askingSeat);
            if (askingOpt.isEmpty() || !askingOpt.get().getPlayerId().equals(playerId)) {
                return Map.<String, Object>of("ok", false, "message", "当前不是你的响应时机");
            }
            GameContext ctx = ctxOpt.get();
            String action = body != null ? body.get("action") : null;
            if ("USE_TAO".equals(action)) {
                String cardId = body != null ? body.get("cardId") : null;
                if (cardId == null || cardId.isBlank()) {
                    return Map.<String, Object>of("ok", false, "message", "请选择要出的桃");
                }
                var player = askingOpt.get();
                var cardOpt = player.getHandCards().stream().filter(c -> c.getId().equals(cardId)).findFirst();
                if (cardOpt.isEmpty()) {
                    return Map.<String, Object>of("ok", false, "message", "手牌中无此牌");
                }
                Card card = cardOpt.get();
                String cardType = card.getRankOrName();
                if (!"桃".equals(cardType) && !"酒".equals(cardType)) {
                    return Map.<String, Object>of("ok", false, "message", "只能出桃或酒救人");
                }
                player.removeHandCard(card);
                ctx.addToDiscardPile(card);
                var targetOpt = ctx.getRoom().getPlayerById(targetId);
                targetOpt.ifPresent(t -> t.setHp(Math.min(t.getMaxHp(), t.getHp() + 1)));
                eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card));
                ctx.addPlayedCard(playerId, cardId, "桃", targetId, targetName);
                ctx.clearPendingDeath();
                webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("PLAY_CARD",
                        Map.of("playerId", playerId, "cardId", cardId, "cardType", card.getRankOrName(),
                                "targetId", targetId, "targetName", targetName)));
                webSocketHandler.broadcastGameContext(roomId, ctx);
                var smOpt = roomService.getStateMachine(roomId);
                if (smOpt.isPresent()) {
                    botService.runBotTurnsUntilHuman(roomId, ctx, smOpt.get());
                    webSocketHandler.broadcastGameContext(roomId, ctx);
                }
                return buildGameStateResponse(ctx, roomId);
            }
            if ("PASS".equals(action) || action == null || action.isBlank()) {
                int nextAsk = ctx.nextSeatIndex(askingSeat);
                var targetOpt = ctx.getRoom().getPlayerById(targetId);
                int targetSeat = targetOpt.map(com.game.domain.Player::getSeatIndex).orElse(-1);
                if (nextAsk == targetSeat) {
                    targetOpt.ifPresent(t -> t.setAlive(false));
                    ctx.clearPendingDeath();
                    webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("PLAYER_DEATH",
                            Map.of("targetId", targetId, "targetName", targetName)));
                } else {
                    ctx.setPendingDeath(targetId, targetName, nextAsk);
                }
                webSocketHandler.broadcastGameContext(roomId, ctx);
                botService.runDyingPoll(roomId, ctx);
                webSocketHandler.broadcastGameContext(roomId, ctx);
                if (ctx.getPendingDeath().isEmpty()) {
                    var smOpt = roomService.getStateMachine(roomId);
                    if (smOpt.isPresent()) {
                        botService.runBotTurnsUntilHuman(roomId, ctx, smOpt.get());
                        webSocketHandler.broadcastGameContext(roomId, ctx);
                    }
                }
                return buildGameStateResponse(ctx, roomId);
            }
            return Map.<String, Object>of("ok", false, "message", "无效的响应");
        });
    }

    /**
     * 结束回合：仅当前玩家可调用，权限交给下一存活玩家，并进入 DRAW→自动摸 2 张→PLAY，全量推送。
     */
    @PostMapping(value = "/{roomId}/endRound", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> endRound(@PathVariable String roomId,
                                              @RequestBody Map<String, String> body) {
        return Mono.fromCallable(() -> {
            var ctxOpt = roomService.getOrCreateContext(roomId);
            var smOpt = roomService.getStateMachine(roomId);
            if (ctxOpt.isEmpty() || smOpt.isEmpty()) {
                return Map.<String, Object>of("ok", false, "message", "对局未开始");
            }
            String playerId = body != null ? body.get("playerId") : null;
            GameContext ctx = ctxOpt.get();
            if (ctx.getCurrentPlayer().map(p -> !p.getPlayerId().equals(playerId)).orElse(true)) {
                return Map.<String, Object>of("ok", false, "message", "仅当前回合玩家可结束回合");
            }
            GameStateMachine sm = smOpt.get();
            ctx.setCurrentSeatIndex(ctx.nextAliveSeatIndex());
            ctx.setAttribute("shaCountThisTurn", 0); // 新回合重置出杀次数
            ctx.setAttribute("jiuUsedThisTurn", 0);  // 新回合重置酒使用次数
            ctx.setAttribute("jiuBuff", false);     // 酒buff未使用则清除
            ctx.setCurrentPhase(Phase.DRAW);
            ctx.incrementRound();
            List<Card> drawn = sm.processDrawPhase(2);
            webSocketHandler.broadcastGameContext(roomId, ctx);
            ctx.getCurrentPlayer().ifPresent(next ->
                    webSocketHandler.sendDrawnCards(roomId, next.getPlayerId(), drawn));
            // 若轮到机器人，自动执行机器人回合直到再次轮到人类
            botService.runBotTurnsUntilHuman(roomId, ctx, sm);
            webSocketHandler.broadcastGameContext(roomId, ctx);
            return buildGameStateResponse(ctx, roomId);
        });
    }

    /** 构建统一的游戏状态响应：hand、players、phase、currentSeatIndex、roundNumber、pendingKill */
    private Map<String, Object> buildGameStateResponse(GameContext ctx, String roomId) {
        var dto = webSocketHandler.buildContextDTO(ctx);
        List<Card> hand = ctx.getCurrentPlayer()
                .map(p -> List.copyOf(p.getHandCards()))
                .orElseGet(List::of);
        var map = new java.util.HashMap<String, Object>();
        map.put("ok", true);
        map.put("roomId", roomId);
        map.put("hand", hand);
        map.put("players", dto.getPlayers() != null ? dto.getPlayers() : List.of());
        map.put("phase", ctx.getCurrentPhase().name());
        map.put("currentSeatIndex", ctx.getCurrentSeatIndex());
        map.put("roundNumber", ctx.getRoundNumber());
        ctx.getPendingKill().ifPresent(pk -> map.put("pendingKill", pk));
        ctx.getPendingDeath().ifPresent(pd -> map.put("pendingDeath", pd));
        map.put("battleCards", ctx.getRecentBattleCards());
        return map;
    }

    /**
     * 推进阶段（兼容旧前端，可选保留）。
     */
    @PostMapping(value = "/{roomId}/advance", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> advancePhase(@PathVariable String roomId) {
        return Mono.fromCallable(() -> {
            var smOpt = roomService.getStateMachine(roomId);
            if (smOpt.isEmpty()) {
                return Map.<String, Object>of("ok", false, "message", "对局未开始");
            }
            GameStateMachine sm = smOpt.get();
            sm.advancePhase();
            GameContext ctx = roomService.getOrCreateContext(roomId).orElseThrow();
            webSocketHandler.broadcastGameContext(roomId, ctx);
            return Map.<String, Object>of("ok", true, "phase", ctx.getCurrentPhase().name(),
                    "currentSeat", ctx.getCurrentSeatIndex(), "round", ctx.getRoundNumber());
        });
    }
}
