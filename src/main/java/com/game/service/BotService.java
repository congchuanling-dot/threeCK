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

/**
 * 简易机器人逻辑。
 * 轮到机器人时：优先出「杀」打人、其次「桃」回血、否则弃「闪」，然后结束回合。
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

    /**
     * 若当前玩家是机器人，执行其回合并切换，直到轮到人类。
     */
    public void runBotTurnsUntilHuman(String roomId, GameContext ctx, GameStateMachine sm) {
        while (ctx.getCurrentPlayer().map(p -> isBot(p.getPlayerId())).orElse(false)) {
            runOneBotTurn(roomId, ctx, sm);
        }
    }

    private void runOneBotTurn(String roomId, GameContext ctx, GameStateMachine sm) {
        Player bot = ctx.getCurrentPlayer().orElse(null);
        if (bot == null || !isBot(bot.getPlayerId())) return;

        List<Card> hand = bot.getHandCards();
        if (hand.isEmpty()) {
            endBotRound(ctx, sm);
            return;
        }

        // 1. 有「杀」则打第一个存活的非bot
        Optional<Player> humanTarget = ctx.getRoom().getPlayers().stream()
                .filter(p -> p.isAlive() && !isBot(p.getPlayerId()))
                .findFirst();
        Card sha = hand.stream().filter(c -> "杀".equals(c.getRankOrName())).findFirst().orElse(null);
        if (sha != null && humanTarget.isPresent()) {
            doPlayCard(roomId, ctx, bot, sha, humanTarget.get().getPlayerId());
            webSocketHandler.broadcastGameContext(roomId, ctx);
            endBotRound(ctx, sm);
            return;
        }

        // 2. 有「桃」且自己受伤则回血
        Card tao = hand.stream().filter(c -> "桃".equals(c.getRankOrName())).findFirst().orElse(null);
        if (tao != null && bot.getHp() < bot.getMaxHp()) {
            doPlayCard(roomId, ctx, bot, tao, bot.getPlayerId());
            webSocketHandler.broadcastGameContext(roomId, ctx);
            endBotRound(ctx, sm);
            return;
        }

        // 3. 弃一张「闪」或任意牌，然后结束回合
        Card discard = hand.stream().filter(c -> "闪".equals(c.getRankOrName())).findFirst()
                .orElse(hand.get(0));
        doPlayCard(roomId, ctx, bot, discard, null);
        webSocketHandler.broadcastGameContext(roomId, ctx);
            endBotRound(ctx, sm);
        return;
    }

    private void doPlayCard(String roomId, GameContext ctx, Player player, Card card, String targetId) {
        player.removeHandCard(card);
        String type = card.getRankOrName();
        if ("杀".equals(type) && targetId != null && !targetId.isBlank()) {
            var targetOpt = ctx.getRoom().getPlayerById(targetId);
            if (targetOpt.isPresent() && targetOpt.get().isAlive()) {
                var target = targetOpt.get();
                eventPublisher.publish(new PlayerDamageEvent(ctx, target, player, 1));
                target.takeDamage(1);
                eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card, target));
                webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("DAMAGE",
                        Map.of("targetId", target.getPlayerId(), "targetName", target.getNickname(),
                                "sourceId", player.getPlayerId(), "sourceName", player.getNickname(), "amount", 1)));
            }
        } else if ("桃".equals(type)) {
            String healId = (targetId != null && !targetId.isBlank()) ? targetId : player.getPlayerId();
            ctx.getRoom().getPlayerById(healId).ifPresent(target -> {
                if (target.isAlive()) {
                    target.setHp(Math.min(target.getMaxHp(), target.getHp() + 1));
                    eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card));
                }
            });
        } else {
            eventPublisher.publish(new PlayerPlayCardEvent(ctx, player, card));
        }
        webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("PLAY_CARD",
                Map.of("playerId", player.getPlayerId(), "cardId", card.getId(), "cardType", type)));
    }

    private void endBotRound(GameContext ctx, GameStateMachine sm) {
        ctx.setCurrentSeatIndex(ctx.nextAliveSeatIndex());
        ctx.setCurrentPhase(com.game.domain.Phase.DRAW);
        ctx.incrementRound();
        List<Card> drawn = sm.processDrawPhase(2);
        ctx.getCurrentPlayer().ifPresent(next ->
                webSocketHandler.sendDrawnCards(ctx.getRoom().getRoomId(), next.getPlayerId(), drawn));
    }
}
