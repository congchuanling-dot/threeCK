package com.game.skill.skills;

import com.game.domain.Card;
import com.game.domain.Player;
import com.game.engine.GameContext;
import com.game.event.GameEventPublisher;
import com.game.event.PlayerPlayCardEvent;
import com.game.skill.SkillEffectLib;
import com.game.websocket.GameMessage;
import com.game.websocket.GameWebSocketHandler;

import java.util.List;
import java.util.Map;

/**
 * 龙胆技能（赵云）：杀可以当闪使用或打出，发动时摸一张牌。
 */
public final class LongdanSkill {

    public static final String SKILL_ID = "longdan";

    private LongdanSkill() {}

    /**
     * 响应杀时，将杀当闪使用。
     * @return 是否成功处理（成功则已清除 pendingKill、移除牌、广播、摸牌）
     */
    public static boolean respondWithShaAsShan(
            String roomId,
            GameContext ctx,
            Player target,
            Card shaCard,
            Map<String, Object> pendingKill,
            GameEventPublisher eventPublisher,
            GameWebSocketHandler webSocketHandler) {
        if (shaCard == null || !"杀".equals(shaCard.getRankOrName())) return false;
        if (!target.getHandCards().contains(shaCard)) return false;

        target.removeHandCard(shaCard);
        ctx.addToDiscardPile(shaCard);
        ctx.clearPendingKill();
        ctx.addPlayedCard(target.getPlayerId(), shaCard.getId(), "闪", null, null);
        eventPublisher.publish(new PlayerPlayCardEvent(ctx, target, shaCard));

        webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("PLAY_CARD",
                Map.of("playerId", target.getPlayerId(), "cardId", shaCard.getId(), "cardType", "闪",
                        "skillId", SKILL_ID, "skillName", "龙胆")));
        webSocketHandler.broadcastToRoom(roomId, GameMessage.broadcast("SHAN_NEGATED",
                Map.of("targetId", target.getPlayerId(), "sourceId", pendingKill.get("sourceId"),
                        "sourceName", pendingKill.get("sourceName"), "message", "出闪抵消了杀（龙胆）")));

        // 发动技能摸一张牌
        List<Card> drawn = SkillEffectLib.drawCards(ctx, target, 1);
        if (!drawn.isEmpty()) {
            webSocketHandler.sendDrawnCards(roomId, target.getPlayerId(), drawn);
        }
        return true;
    }
}
