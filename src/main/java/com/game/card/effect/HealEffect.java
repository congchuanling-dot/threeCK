package com.game.card.effect;

import com.game.card.CardContext;
import com.game.card.CardTarget;

/**
 * 回复体力效果。
 */
public class HealEffect implements CardEffect {

    private final int amount;

    public HealEffect(int amount) {
        this.amount = Math.max(1, amount);
    }

    @Override
    public void apply(CardTarget source, CardTarget target, CardContext context) {
        CardTarget recipient = target != null ? target : source;
        if (recipient == null) return;
        recipient.setHp(Math.min(recipient.getHp() + amount, recipient.getMaxHp()));
    }
}
