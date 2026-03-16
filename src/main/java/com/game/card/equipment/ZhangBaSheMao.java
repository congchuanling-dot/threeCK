package com.game.card.equipment;

import com.game.domain.Suit;

/**
 * 丈八蛇矛：你可以将两张手牌当杀使用。
 */
public class ZhangBaSheMao extends Weapon {

    public ZhangBaSheMao(String id, Suit suit, int number) {
        super(id, "丈八蛇矛", "你可以将两张手牌当杀使用", suit, number, 1);
    }
}
