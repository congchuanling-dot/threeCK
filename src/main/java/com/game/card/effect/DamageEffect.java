package com.game.card.effect;

import com.game.card.CardContext;
import com.game.card.CardTarget;

/**
 * 造成伤害效果。
 */
public class DamageEffect implements CardEffect {

    private final int amount;
    private final boolean fireDamage;

    public DamageEffect(int amount) {
        this(amount, false);
    }

    public DamageEffect(int amount, boolean fireDamage) {
        this.amount = Math.max(0, amount);
        this.fireDamage = fireDamage;
    }

    @Override
    public void apply(CardTarget source, CardTarget target, CardContext context) {
        if (target != null && target.isAlive()) {
            target.takeDamage(amount);
        }
    }
}
