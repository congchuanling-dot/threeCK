package com.game.card.equipment;

import com.game.domain.Suit;

/**
 * 贯石斧：当你使用杀被闪抵消时，可以弃两张牌使该杀依然造成伤害。
 */
public class GuanShiFu extends Weapon {

    public GuanShiFu(String id, Suit suit, int number) {
        super(id, "贯石斧", "当你使用杀被闪抵消时，可以弃两张牌使该杀依然造成伤害", suit, number, 1);
    }
}
