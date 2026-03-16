package com.game.card.equipment;

import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.domain.Suit;

/**
 * 坐骑牌抽象基类。
 * @param isOffensive true 为进攻马（-1），false 为防御马（+1）
 */
public abstract class Horse extends EquipmentCard {

    /** true=进攻马(-1)，false=防御马(+1) */
    protected final boolean isOffensive;

    protected Horse(String id, String name, String description, Suit suit, int number, boolean isOffensive) {
        super(id, name, description, suit, number);
        this.isOffensive = isOffensive;
    }

    @Override
    protected void equip(CardTarget wearer, CardContext context) {
        if (isOffensive) {
            if (wearer.getOffensiveHorse() != null) {
                context.addToDiscardPile(wearer.getOffensiveHorse());
            }
            wearer.setOffensiveHorse(this);
        } else {
            if (wearer.getDefensiveHorse() != null) {
                context.addToDiscardPile(wearer.getDefensiveHorse());
            }
            wearer.setDefensiveHorse(this);
        }
    }
}
