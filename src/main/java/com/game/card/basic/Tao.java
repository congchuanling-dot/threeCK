package com.game.card.basic;

import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.card.effect.HealEffect;
import com.game.domain.Suit;

/**
 * 桃：回复1点体力。
 */
public class Tao extends BasicCard {

    public Tao(String id, Suit suit, int number) {
        super(id, "桃", "回复1点体力", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        CardTarget recipient = target != null ? target : source;
        if (recipient != null) {
            new HealEffect(1).apply(source, recipient, context);
        }
    }
}
