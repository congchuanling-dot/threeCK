package com.game.card.equipment;

import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.domain.Suit;

/**
 * 武器牌抽象基类。
 * 装备后提供攻击距离或特殊效果。
 */
public abstract class Weapon extends EquipmentCard {

    /** 攻击距离，默认为 1 */
    protected final int attackRange;

    protected Weapon(String id, String name, String description, Suit suit, int number, int attackRange) {
        super(id, name, description, suit, number);
        this.attackRange = Math.max(1, attackRange);
    }

    @Override
    protected void equip(CardTarget wearer, CardContext context) {
        if (wearer.getWeapon() != null) {
            context.addToDiscardPile(wearer.getWeapon());
        }
        wearer.setWeapon(this);
    }
}
