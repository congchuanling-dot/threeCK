package com.game.card.trick;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.domain.Suit;

import java.util.ArrayList;
import java.util.List;

/**
 * 五谷丰登：从牌堆亮出等同于存活角色数量的牌，每名角色依次选择一张获得。
 * （简化：按座位顺序每人摸一张）
 */
public class WuGuFengDeng extends TrickCard {

    public WuGuFengDeng(String id, Suit suit, int number) {
        super(id, "五谷丰登", "从牌堆亮出等同于存活角色数量的牌，每名角色依次选择一张获得", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        List<CardTarget> alive = context.getAlivePlayers();
        int n = alive.size();
        List<Card> drawn = context.drawFromPile(n);
        for (int i = 0; i < drawn.size() && i < alive.size(); i++) {
            alive.get(i).addHandCard(drawn.get(i));
        }
    }
}
