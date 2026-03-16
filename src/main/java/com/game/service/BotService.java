package com.game.service;

import com.game.domain.Card;
import com.game.domain.Player;
import com.game.engine.GameContext;
import com.game.service.CardPlayService;
import com.game.engine.GameStateMachine;
import com.game.event.GameEventPublisher;
import com.game.event.PlayerDamageEvent;
import com.game.event.PlayerPlayCardEvent;
import com.game.websocket.GameMessage;
import com.game.websocket.GameWebSocketHandler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static java.lang.Thread.sleep;

/**
 * 简易机器人逻辑。
 * 轮到机器人时：优先出「杀」打人、其次「桃」回血；闪不能主动弃置，只能响应杀；每张牌间隔1秒。
 */
@Service
public class BotService {

    private static final String BOT_PREFIX = "BOT_";
    private static final Random RND = new Random();

    private final GameEventPublisher eventPublisher;
    private final GameWebSocketHandler webSocketHandler;
    private final CardPlayService cardPlayService;

    public BotService(GameEventPublisher eventPublisher, GameWebSocketHandler webSocketHandler,
                      CardPlayService cardPlayService) {
        this.eventPublisher = eventPublisher;
        this.webSocketHandler = webSocketHandler;
        this.cardPlayService = cardPlayService;
    }

    /** 判断是否为机器人 */
    public static boolean isBot(String playerId) {
        return playerId != null && playerId.startsWith(BOT_PREFIX);
    }

    private static final int BOT_DELAY_MS = 1000;

    private void botDelay() {
        try {
            sleep(BOT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 被杀目标为机器人时，延迟 1 秒后出闪或承受伤害。
     */
    public void respondToKill(String roomId, GameContext ctx) {
        var pendingOpt = ctx.getPendingKill();
        if (pendingOpt.isEmpty()) return;
        var pending = pendingOpt.get();
        String targetId = (String) pending.get("targetId");
        var targetOpt = ctx.getRoom().getPlayerById(targetId);
        if (targetOpt.isEmpty() || !isBot(targetId)) return;
        try {
            sleep(BOT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        var bot = targetOpt.get();
        var shan = bot.getHandCards().stream().filter(c -> "闪".equals(c.getRankOrName())).findFirst().orElse(null);
        if (shan != null) {
            bot.removeHandCard(shan);
            ctx.addToDiscardPile(shan);
            ctx.clearPendingKill();
            eventPublisher.publish(new PlayerPlayCardEvent(ctx, bot, shan));
            webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("SHAN_NEGATED",
                    Map.of("targetId", targetId, "message", "机器人出闪抵消了杀")));
        } else {
            int amount = ((Number) pending.getOrDefault("amount", 1)).intValue();
            var sourceOpt = ctx.getRoom().getPlayerById((String) pending.get("sourceId"));
            sourceOpt.ifPresent(source ->
                    eventPublisher.publish(new PlayerDamageEvent(ctx, bot, source, amount)));
            ctx.clearPendingKill();
            webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("DAMAGE",
                    Map.of("targetId", targetId, "targetName", pending.get("targetName"),
                            "sourceId", pending.get("sourceId"), "sourceName", pending.get("sourceName"), "amount", amount)));
            // 若机器人濒死，进行濒死轮询
            if (ctx.getPendingDeath().isPresent()) {
                webSocketHandler.broadcastGameContext(roomId, ctx);
                runDyingPoll(roomId, ctx);
            }
        }
    }

    /**
     * 濒死轮询：从濒死者开始依次询问每位玩家是否出桃。轮到机器人时自动响应；轮到人类时返回等待 HTTP。
     */
    public void runDyingPoll(String roomId, GameContext ctx) {
        while (ctx.getPendingDeath().isPresent()) {
            var pending = ctx.getPendingDeath().get();
            int askingSeat = ((Number) pending.get("askingSeatIndex")).intValue();
            var playerOpt = ctx.getRoom().getPlayerBySeat(askingSeat);
            if (playerOpt.isEmpty()) break;
            Player p = playerOpt.get();
            if (isBot(p.getPlayerId())) {
                botDelay();
                respondToDying(roomId, ctx);
                webSocketHandler.broadcastGameContext(roomId, ctx);
            } else {
                return; // 轮到人类，等待 HTTP respondDying
            }
        }
    }

    /**
     * 机器人响应濒死：有桃则用桃救（可自救），否则跳过。
     */
    public void respondToDying(String roomId, GameContext ctx) {
        var pendingOpt = ctx.getPendingDeath();
        if (pendingOpt.isEmpty()) return;
        var pending = pendingOpt.get();
        String targetId = (String) pending.get("targetId");
        int askingSeat = ((Number) pending.get("askingSeatIndex")).intValue();
        var askingOpt = ctx.getRoom().getPlayerBySeat(askingSeat);
        if (askingOpt.isEmpty()) return;
        Player asking = askingOpt.get();
        if (!isBot(asking.getPlayerId())) return;

        var tao = asking.getHandCards().stream().filter(c -> "桃".equals(c.getRankOrName())).findFirst().orElse(null);
        if (tao != null) {
            asking.removeHandCard(tao);
            ctx.addToDiscardPile(tao);
            var targetOpt = ctx.getRoom().getPlayerById(targetId);
            targetOpt.ifPresent(t -> {
                t.setHp(Math.min(t.getMaxHp(), t.getHp() + 1));
                eventPublisher.publish(new PlayerPlayCardEvent(ctx, asking, tao));
            });
            ctx.addPlayedCard(asking.getPlayerId(), tao.getId(), "桃", targetId, (String) pending.get("targetName"));
            ctx.clearPendingDeath();
            webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("PLAY_CARD",
                    Map.of("playerId", asking.getPlayerId(), "cardId", tao.getId(), "cardType", "桃",
                            "targetId", targetId, "targetName", pending.get("targetName"))));
        } else {
            // 跳过，询问下一人
            String targetName = (String) pending.get("targetName");
            int nextAsk = ctx.nextSeatIndex(askingSeat);
            var targetOpt = ctx.getRoom().getPlayerById(targetId);
            int targetSeat = targetOpt.map(Player::getSeatIndex).orElse(-1);
            if (nextAsk == targetSeat) {
                targetOpt.ifPresent(t -> t.setAlive(false));
                ctx.clearPendingDeath();
                webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("PLAYER_DEATH",
                        Map.of("targetId", targetId, "targetName", targetName)));
            } else {
                ctx.setPendingDeath(targetId, targetName, nextAsk);
            }
        }
    }

    /**
     * 若当前玩家是机器人，执行其回合并切换，直到轮到人类。
     */
    public void runBotTurnsUntilHuman(String roomId, GameContext ctx, GameStateMachine sm) {
        while (ctx.getCurrentPlayer().map(p -> isBot(p.getPlayerId())).orElse(false)) {
            if (runOneBotTurn(roomId, ctx, sm)) break; // 杀人类待响应，暂停
        }
    }

    /** @return true 表示需暂停（杀人类待响应） */
    private boolean runOneBotTurn(String roomId, GameContext ctx, GameStateMachine sm) {
        Player bot = ctx.getCurrentPlayer().orElse(null);
        if (bot == null || !isBot(bot.getPlayerId())) return false;

        List<Card> hand = bot.getHandCards();
        if (hand.isEmpty()) {
            botDelay();
            endBotRound(roomId, ctx, sm);
            return false;
        }

        Optional<Player> humanTarget = ctx.getRoom().getPlayers().stream()
                .filter(p -> p.isAlive() && !isBot(p.getPlayerId()))
                .findFirst();

        // 0. 锦囊/装备：无中生有、桃园结义、南蛮入侵等无目标；装备给自己；需目标锦囊打人类
        Card trickOrEquip = hand.stream()
                .filter(c -> CardPlayService.isHandledByCardSystem(c.getRankOrName()))
                .findFirst()
                .orElse(null);
        if (trickOrEquip != null) {
            String name = trickOrEquip.getRankOrName();
            String tid = null;
            if (CardPlayService.isEquipment(name)) {
                tid = bot.getPlayerId();
            } else if (CardPlayService.trickNeedsTarget(name)) {
                if (humanTarget.isPresent()) tid = humanTarget.get().getPlayerId();
            }
            // 无目标锦囊：无中生有、南蛮入侵、万箭齐发、五谷丰登、桃园结义等
            if (tid != null || !CardPlayService.trickNeedsTarget(name)) {
                if (tid == null && "桃园结义".equals(name)
                        && !ctx.getRoom().getPlayers().stream().anyMatch(p -> p.isAlive() && p.getHp() < p.getMaxHp())) {
                    // 桃园结义但无人受伤，跳过
                } else {
                    botDelay();
                    doPlayTrickOrEquipment(roomId, ctx, bot, trickOrEquip, tid);
                    webSocketHandler.broadcastGameContext(roomId, ctx);
                    return false;
                }
            }
        }

        // 1. 有「杀」则打第一个存活的非bot（每回合1次，诸葛连弩无限）
        int shaCount = ctx.getAttribute("shaCountThisTurn").map(o -> ((Number) o).intValue()).orElse(0);
        boolean canPlaySha = shaCount < 1 || com.game.card.adapter.PlayerCardTargetAdapter.hasZhuGeLianNu(bot.getPlayerId());
        Card sha = hand.stream().filter(c -> "杀".equals(c.getRankOrName())).findFirst().orElse(null);
        if (sha != null && humanTarget.isPresent() && canPlaySha) {
            botDelay();
            boolean pendingHuman = doPlayCard(roomId, ctx, bot, sha, humanTarget.get().getPlayerId());
            webSocketHandler.broadcastGameContext(roomId, ctx);
            if (pendingHuman) return true; // 等待人类出闪或承受，暂停
            if (ctx.getPendingDeath().isPresent()) return true; // 机器人濒死，等待人类响应出桃
            botDelay();
            endBotRound(roomId, ctx, sm);
            return false;
        }

        // 2. 有「桃」且自己受伤则回血
        Card tao = hand.stream().filter(c -> "桃".equals(c.getRankOrName())).findFirst().orElse(null);
        if (tao != null && bot.getHp() < bot.getMaxHp()) {
            botDelay();
            doPlayCard(roomId, ctx, bot, tao, bot.getPlayerId());
            webSocketHandler.broadcastGameContext(roomId, ctx);
            botDelay();
            endBotRound(roomId, ctx, sm);
            return false;
        }

        // 3. 无杀无桃可出，直接结束回合（闪不能主动弃置，仅能响应杀）
        botDelay();
        endBotRound(roomId, ctx, sm);
        return false;
    }

    /** 机器人出锦囊或装备，执行卡牌效果 */
    private void doPlayTrickOrEquipment(String roomId, GameContext ctx, Player player, Card card, String targetId) {
        player.removeHandCard(card);
        ctx.addToDiscardPile(card);
        var target = (targetId != null && !targetId.isBlank())
                ? ctx.getRoom().getPlayerById(targetId).orElse(null) : null;
        cardPlayService.executeCardEffect(ctx, player, card, target);
        eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card));
        String tid = target != null ? target.getPlayerId() : null;
        String tname = target != null ? target.getNickname() : null;
        ctx.addPlayedCard(player.getPlayerId(), card.getId(), card.getRankOrName(), tid, tname);
        webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("PLAY_CARD",
                Map.of("playerId", player.getPlayerId(), "cardId", card.getId(), "cardType", card.getRankOrName())));
    }

    /** 若 bot 杀的是人类，设置 pendingKill 等待响应，返回 true 表示应暂停 bot 回合 */
    private boolean doPlayCard(String roomId, GameContext ctx, Player player, Card card, String targetId) {
        player.removeHandCard(card);
        ctx.addToDiscardPile(card);
        String type = card.getRankOrName();
        if ("杀".equals(type) && targetId != null && !targetId.isBlank()) {
            var targetOpt = ctx.getRoom().getPlayerById(targetId);
            if (targetOpt.isPresent() && targetOpt.get().isAlive()) {
                var target = targetOpt.get();
                int shaCount = ctx.getAttribute("shaCountThisTurn").map(o -> ((Number) o).intValue()).orElse(0);
                ctx.setAttribute("shaCountThisTurn", shaCount + 1);
                ctx.addPlayedCard(player.getPlayerId(), card.getId(), "杀", target.getPlayerId(), target.getNickname());
                eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card, target));
                webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("PLAY_CARD",
                        Map.of("playerId", player.getPlayerId(), "cardId", card.getId(), "cardType", "杀",
                                "targetId", target.getPlayerId(), "targetName", target.getNickname())));
                if (isBot(target.getPlayerId())) {
                    eventPublisher.publish(new PlayerDamageEvent(ctx, target, player, 1));
                    webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("DAMAGE",
                            Map.of("targetId", target.getPlayerId(), "targetName", target.getNickname(),
                                    "sourceId", player.getPlayerId(), "sourceName", player.getNickname(), "amount", 1)));
                    if (ctx.getPendingDeath().isPresent()) {
                        webSocketHandler.broadcastGameContext(roomId, ctx);
                        runDyingPoll(roomId, ctx);
                    }
                } else {
                    ctx.setPendingKill(target.getPlayerId(), player.getPlayerId(), player.getNickname(), target.getNickname(), 1);
                    return true;
                }
            }
        } else if ("桃".equals(type)) {
            String healId = (targetId != null && !targetId.isBlank()) ? targetId : player.getPlayerId();
            ctx.addPlayedCard(player.getPlayerId(), card.getId(), "桃", healId, null);
            ctx.getRoom().getPlayerById(healId).ifPresent(target -> {
                if (target.isAlive()) {
                    target.setHp(Math.min(target.getMaxHp(), target.getHp() + 1));
                    eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card));
                }
            });
        } else {
            eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card));
            ctx.addPlayedCard(player.getPlayerId(), card.getId(), type, null, null);
        }
        webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("PLAY_CARD",
                Map.of("playerId", player.getPlayerId(), "cardId", card.getId(), "cardType", type)));
        return false;
    }

    private void endBotRound(String roomId, GameContext ctx, GameStateMachine sm) {
        ctx.setCurrentSeatIndex(ctx.nextAliveSeatIndex());
        ctx.setAttribute("shaCountThisTurn", 0); // 新回合重置出杀次数
        ctx.setCurrentPhase(com.game.domain.Phase.DRAW);
        ctx.incrementRound();
        sm.processDrawPhase(2);
        // 直接广播完整状态（含新摸的牌），不单独发 DRAWN_CARDS，避免前端重复更新手牌
        webSocketHandler.broadcastGameContext(roomId, ctx);
    }
}
