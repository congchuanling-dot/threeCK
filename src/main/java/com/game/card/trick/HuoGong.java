package com.game.card.trick;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.card.effect.DamageEffect;
import com.game.domain.Suit;

import java.util.List;

/**
 * 火攻：展示目标角色的一张手牌，你可以弃置一张相同花色的牌，对其造成1点火焰伤害。
 * （简化：若 source 有相同花色牌，弃一张并对目标造成1点火焰伤害）
 */
public class HuoGong extends TrickCard {

    public HuoGong(String id, Suit suit, int number) {
        super(id, "火攻", "展示目标角色的一张手牌，你可以弃置一张相同花色的牌，对其造成1点火焰伤害", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        if (target == null) return;
        List<? extends Card> targetHand = target.getHandCards();
        if (targetHand.isEmpty()) return;
        Card shown = targetHand.get(0);
        Suit suit = shown.getSuit();
        Card toDiscard = findSameSuit(source.getHandCards(), suit);
        if (toDiscard != null) {
            source.removeHandCard(toDiscard);
            context.addToDiscardPile(toDiscard);
            new DamageEffect(1, true).apply(source, target, context);
        }
    }

    private Card findSameSuit(List<? extends Card> hand, Suit suit) {
        for (Card c : hand) {
            if (c != null && c.getSuit() == suit) return c;
        }
        return null;
    }
}
