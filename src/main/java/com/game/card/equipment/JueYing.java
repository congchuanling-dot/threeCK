package com.game.card.equipment;

import com.game.domain.Suit;

/** 绝影：防御马 */
public class JueYing extends Horse {
    public JueYing(String id, Suit suit, int number) {
        super(id, "绝影", "其他角色计算与你的距离 +1", suit, number, false);
    }
}
