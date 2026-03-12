package com.game.controller;

import com.game.domain.Card;
import com.game.domain.Phase;
import com.game.engine.GameContext;
import com.game.engine.GameStateMachine;
import com.game.event.GameEventPublisher;
import com.game.event.PlayerDamageEvent;
import com.game.event.PlayerPlayCardEvent;
import com.game.service.RoomService;
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

    public GameController(RoomService roomService,
                          GameEventPublisher eventPublisher,
                          GameWebSocketHandler webSocketHandler,
                          com.game.engine.DefaultGameStarter defaultGameStarter) {
        this.roomService = roomService;
        this.eventPublisher = eventPublisher;
        this.webSocketHandler = webSocketHandler;
        this.defaultGameStarter = defaultGameStarter;
    }

    /**
     * 开局：仅房主可触发。初始化牌堆（52 张杀/闪/桃）、发 4 张、当前玩家摸 2 张进入 PLAY 阶段，并全量推送。
     */
    @PostMapping(value = "/{roomId}/start", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> startGame(@PathVariable String roomId,
                                               @RequestBody(required = false) Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            String playerId = body != null && body.get("playerId") != null
                    ? body.get("playerId").toString() : null;
            var resultOpt = roomService.startGame(roomId, playerId, defaultGameStarter);
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
            List<Card> currentHand = ctx.getCurrentPlayer()
                    .map(p -> List.copyOf(p.getHandCards()))
                    .orElseGet(List::of);
            return Map.<String, Object>of(
                    "ok", true,
                    "roomId", roomId,
                    "phase", ctx.getCurrentPhase().name(),
                    "hand", currentHand
            );
        });
    }

    /**
     * 出牌：当前阶段为 PLAY 时有效。杀→目标 -1 血；桃→目标 +1 血（上限封顶）；闪→仅弃置。
     */
    @PostMapping(value = "/{roomId}/play", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> playCard(@PathVariable String roomId,
                                              @RequestBody Map<String, String> body) {
        return Mono.fromCallable(() -> {
            var ctxOpt = roomService.getOrCreateContext(roomId);
            var smOpt = roomService.getStateMachine(roomId);
            if (ctxOpt.isEmpty() || smOpt.isEmpty() || !smOpt.get().canPlayCard()) {
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
            if ("杀".equals(type)) {
                String realTargetId = (targetId == null || targetId.isBlank()) ? playerId : targetId;
                var targetOpt = ctx.getRoom().getPlayerById(realTargetId);
                if (targetOpt.isEmpty() || !targetOpt.get().isAlive()) {
                    return Map.<String, Object>of("ok", false, "message", "目标无效");
                }
                var target = targetOpt.get();
                player.removeHandCard(card);
                eventPublisher.publish(new PlayerDamageEvent(ctx, target, player, 1));
                target.takeDamage(1);
                eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card, target));
                webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("DAMAGE",
                        Map.of("targetId", target.getPlayerId(), "targetName", target.getNickname(),
                                "sourceId", player.getPlayerId(), "sourceName", player.getNickname(), "amount", 1)));
            } else if ("桃".equals(type)) {
                String healTarget = (targetId != null && !targetId.isBlank()) ? targetId : playerId;
                var targetOpt = ctx.getRoom().getPlayerById(healTarget);
                if (targetOpt.isEmpty() || !targetOpt.get().isAlive()) {
                    return Map.<String, Object>of("ok", false, "message", "目标无效");
                }
                var target = targetOpt.get();
                player.removeHandCard(card);
                int before = target.getHp();
                target.setHp(Math.min(target.getMaxHp(), before + 1));
                eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card));
            } else if ("闪".equals(type)) {
                player.removeHandCard(card);
                eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card));
            } else {
                player.removeHandCard(card);
                eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card));
            }
            webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("PLAY_CARD",
                    Map.of("playerId", playerId, "cardId", cardId, "cardType", type)));
            webSocketHandler.broadcastGameContext(roomId, ctx);
            return Map.<String, Object>of("ok", true);
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
            ctx.setCurrentPhase(Phase.DRAW);
            ctx.incrementRound();
            List<Card> drawn = sm.processDrawPhase(2);
            webSocketHandler.broadcastGameContext(roomId, ctx);
            ctx.getCurrentPlayer().ifPresent(next ->
                    webSocketHandler.sendDrawnCards(roomId, next.getPlayerId(), drawn));
            return Map.<String, Object>of("ok", true, "phase", ctx.getCurrentPhase().name(),
                    "currentSeat", ctx.getCurrentSeatIndex(), "round", ctx.getRoundNumber());
        });
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
