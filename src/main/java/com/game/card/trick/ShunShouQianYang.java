package com.game.card.trick;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.domain.Suit;

import java.util.List;

/**
 * 顺手牵羊：获得距离1以内的一名角色的一张牌。
 * （简化实现：获得目标一张手牌，距离校验由调用方负责）
 */
public class ShunShouQianYang extends TrickCard {

    public ShunShouQianYang(String id, Suit suit, int number) {
        super(id, "顺手牵羊", "获得距离1以内的一名角色的一张牌", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        if (target == null || !target.isAlive()) return;
        List<? extends Card> hand = target.getHandCards();
        if (!hand.isEmpty()) {
            Card c = hand.get(0);
            target.removeHandCard(c);
            source.addHandCard(c);
        }
    }
}
