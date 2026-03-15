package com.game.service;

import com.game.domain.Card;
import com.game.domain.Player;
import com.game.engine.GameContext;
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

    public BotService(GameEventPublisher eventPublisher, GameWebSocketHandler webSocketHandler) {
        this.eventPublisher = eventPublisher;
        this.webSocketHandler = webSocketHandler;
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

        // 1. 有「杀」则打第一个存活的非bot
        Optional<Player> humanTarget = ctx.getRoom().getPlayers().stream()
                .filter(p -> p.isAlive() && !isBot(p.getPlayerId()))
                .findFirst();
        Card sha = hand.stream().filter(c -> "杀".equals(c.getRankOrName())).findFirst().orElse(null);
        if (sha != null && humanTarget.isPresent()) {
            botDelay();
            boolean pendingHuman = doPlayCard(roomId, ctx, bot, sha, humanTarget.get().getPlayerId());
            webSocketHandler.broadcastGameContext(roomId, ctx);
            if (pendingHuman) return true; // 等待人类出闪或承受，暂停
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

    /** 若 bot 杀的是人类，设置 pendingKill 等待响应，返回 true 表示应暂停 bot 回合 */
    private boolean doPlayCard(String roomId, GameContext ctx, Player player, Card card, String targetId) {
        player.removeHandCard(card);
        ctx.addToDiscardPile(card);
        String type = card.getRankOrName();
        if ("杀".equals(type) && targetId != null && !targetId.isBlank()) {
            var targetOpt = ctx.getRoom().getPlayerById(targetId);
            if (targetOpt.isPresent() && targetOpt.get().isAlive()) {
                var target = targetOpt.get();
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
        ctx.setCurrentPhase(com.game.domain.Phase.DRAW);
        ctx.incrementRound();
        sm.processDrawPhase(2);
        // 直接广播完整状态（含新摸的牌），不单独发 DRAWN_CARDS，避免前端重复更新手牌
        webSocketHandler.broadcastGameContext(roomId, ctx);
    }
}
