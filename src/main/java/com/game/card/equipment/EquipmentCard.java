package com.game.card.equipment;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.card.CardType;
import com.game.domain.Suit;

/**
 * 装备牌抽象基类。
 * 装备进入装备区后提供持续效果。
 */
public abstract class EquipmentCard extends Card {

    protected EquipmentCard(String id, String name, String description, Suit suit, int number) {
        super(id, name, description, CardType.EQUIPMENT, suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        CardTarget wearer = target != null ? target : source;
        if (wearer == null) return;
        equip(wearer, context);
    }

    /**
     * 装备到此目标。子类可覆盖以处理不同装备槽（武器/防具/马）。
     */
    protected abstract void equip(CardTarget wearer, CardContext context);
}
