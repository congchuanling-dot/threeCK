package com.game.card.effect;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardTarget;

import java.util.List;

/**
 * 摸牌效果。
 */
public class DrawCardEffect implements CardEffect {

    private final int count;

    public DrawCardEffect(int count) {
        this.count = Math.max(1, count);
    }

    @Override
    public void apply(CardTarget source, CardTarget target, CardContext context) {
        CardTarget recipient = target != null ? target : source;
        if (recipient == null) return;
        List<Card> drawn = context.drawFromPile(count);
        recipient.addHandCards(drawn);
    }
}
