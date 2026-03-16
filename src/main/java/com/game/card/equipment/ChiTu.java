package com.game.card.equipment;

import com.game.domain.Suit;

/** 赤兔：进攻马，你计算与其他角色的距离 -1 */
public class ChiTu extends Horse {
    public ChiTu(String id, Suit suit, int number) {
        super(id, "赤兔", "你计算与其他角色的距离 -1", suit, number, true);
    }
}
