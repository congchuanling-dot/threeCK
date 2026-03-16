package com.game.card.equipment;

import com.game.domain.Suit;

/** 大宛：进攻马 */
public class DaWan extends Horse {
    public DaWan(String id, Suit suit, int number) {
        super(id, "大宛", "你计算与其他角色的距离 -1", suit, number, true);
    }
}
