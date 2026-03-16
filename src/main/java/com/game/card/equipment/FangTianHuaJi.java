package com.game.card.equipment;

import com.game.domain.Suit;

/**
 * 方天画戟：当你使用的杀是最后一张手牌时，可以指定额外两个目标。
 */
public class FangTianHuaJi extends Weapon {

    public FangTianHuaJi(String id, Suit suit, int number) {
        super(id, "方天画戟", "当你使用的杀是最后一张手牌时，可以指定额外两个目标", suit, number, 1);
    }
}
