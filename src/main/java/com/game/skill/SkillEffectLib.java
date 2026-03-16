package com.game.skill;

import com.game.domain.Card;
import com.game.domain.Player;
import com.game.engine.GameContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能效果工具库。封装通用效果（摸牌、弃牌等），便于开发新技能时调库组合。
 * 面向未来 AI 生成技能时的可扩展性设计。
 */
public final class SkillEffectLib {

    private SkillEffectLib() {}

    /** 令指定玩家从牌堆摸 count 张牌 */
    public static List<Card> drawCards(GameContext ctx, Player player, int count) {
        List<Card> drawn = ctx.drawFromPile(count);
        if (!drawn.isEmpty()) {
            player.addHandCards(drawn);
        }
        return drawn;
    }

    /** 将牌加入弃牌堆 */
    public static void addToDiscardPile(GameContext ctx, Card card) {
        if (card != null) ctx.addToDiscardPile(card);
    }

    /** 从玩家手牌移除指定牌并加入弃牌堆 */
    public static boolean discardHandCard(GameContext ctx, Player player, Card card) {
        if (card == null) return false;
        if (!player.getHandCards().contains(card)) return false;
        player.removeHandCard(card);
        ctx.addToDiscardPile(card);
        return true;
    }
}
