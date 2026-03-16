package com.game.card.trick;

import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.card.effect.DrawCardEffect;
import com.game.domain.Suit;

/**
 * 无中生有：使用后摸两张牌。
 */
public class WuZhongShengYou extends TrickCard {

    public WuZhongShengYou(String id, Suit suit, int number) {
        super(id, "无中生有", "使用后摸两张牌", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        new DrawCardEffect(2).apply(source, source, context);
    }
}
