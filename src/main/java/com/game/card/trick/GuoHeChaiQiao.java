package com.game.card.trick;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.domain.Suit;

import java.util.ArrayList;
import java.util.List;

/**
 * 过河拆桥：弃置目标角色的一张牌（手牌、装备区或判定区）。
 * （简化实现：优先弃置手牌，无手牌则弃装备）
 */
public class GuoHeChaiQiao extends TrickCard {

    public GuoHeChaiQiao(String id, Suit suit, int number) {
        super(id, "过河拆桥", "弃置目标角色的一张牌（手牌、装备区或判定区）", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        if (target == null || !target.isAlive()) return;
        List<? extends Card> hand = target.getHandCards();
        if (!hand.isEmpty()) {
            Card c = hand.get(0);
            target.removeHandCard(c);
            context.addToDiscardPile(c);
        } else {
            List<Card> toDiscard = new ArrayList<>();
            if (target.getWeapon() != null) toDiscard.add(target.getWeapon());
            if (target.getArmor() != null) toDiscard.add(target.getArmor());
            if (target.getOffensiveHorse() != null) toDiscard.add(target.getOffensiveHorse());
            if (target.getDefensiveHorse() != null) toDiscard.add(target.getDefensiveHorse());
            if (!toDiscard.isEmpty()) {
                Card c = toDiscard.get(0);
                if (target.getWeapon() == c) target.setWeapon(null);
                else if (target.getArmor() == c) target.setArmor(null);
                else if (target.getOffensiveHorse() == c) target.setOffensiveHorse(null);
                else if (target.getDefensiveHorse() == c) target.setDefensiveHorse(null);
                context.addToDiscardPile(c);
            }
        }
    }
}
