package com.game.card.equipment;

import com.game.domain.Suit;

/** 紫骍：进攻马 */
public class ZiXing extends Horse {
    public ZiXing(String id, Suit suit, int number) {
        super(id, "紫骍", "你计算与其他角色的距离 -1", suit, number, true);
    }
}
