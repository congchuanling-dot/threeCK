package com.game.card.effect;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardTarget;

import java.util.List;

/**
 * 弃牌效果。将目标的牌弃入弃牌堆。
 */
public class DiscardCardEffect implements CardEffect {

    private final int count;

    public DiscardCardEffect(int count) {
        this.count = Math.max(1, count);
    }

    @Override
    public void apply(CardTarget source, CardTarget target, CardContext context) {
        if (target == null) return;
        for (int i = 0; i < count; i++) {
            List<? extends Card> hand = target.getHandCards();
            if (hand.isEmpty()) break;
            Card c = hand.get(0);
            target.removeHandCard(c);
            context.addToDiscardPile(c);
        }
    }
}
