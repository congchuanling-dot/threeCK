package com.game.card.equipment;

import com.game.domain.Suit;

/** 的卢：防御马 */
public class DiLu extends Horse {
    public DiLu(String id, Suit suit, int number) {
        super(id, "的卢", "其他角色计算与你的距离 +1", suit, number, false);
    }
}
