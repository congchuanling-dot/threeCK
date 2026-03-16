package com.game.card.trick;

import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.card.effect.HealEffect;
import com.game.domain.Suit;

import java.util.List;

/**
 * 桃园结义：所有角色回复1点体力。
 */
public class TaoYuanJieYi extends TrickCard {

    public TaoYuanJieYi(String id, Suit suit, int number) {
        super(id, "桃园结义", "所有角色回复1点体力", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        List<CardTarget> alive = context.getAlivePlayers();
        HealEffect heal = new HealEffect(1);
        for (CardTarget t : alive) {
            heal.apply(source, t, context);
        }
    }
}
