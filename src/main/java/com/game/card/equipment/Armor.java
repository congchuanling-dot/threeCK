package com.game.card.equipment;

import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.domain.Suit;

/**
 * 防具牌抽象基类。
 */
public abstract class Armor extends EquipmentCard {

    protected Armor(String id, String name, String description, Suit suit, int number) {
        super(id, name, description, suit, number);
    }

    @Override
    protected void equip(CardTarget wearer, CardContext context) {
        if (wearer.getArmor() != null) {
            context.addToDiscardPile(wearer.getArmor());
        }
        wearer.setArmor(this);
    }
}
