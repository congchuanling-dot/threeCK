package com.game.card.equipment;

import com.game.domain.Suit;

/**
 * 寒冰剑：你使用杀造成伤害时，可以改为弃置目标两张牌。
 */
public class HanBingJian extends Weapon {

    public HanBingJian(String id, Suit suit, int number) {
        super(id, "寒冰剑", "你使用杀造成伤害时，可以改为弃置目标两张牌", suit, number, 1);
    }
}
