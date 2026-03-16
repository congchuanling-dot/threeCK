package com.game.card.equipment;

import com.game.domain.Suit;

/**
 * 青龙偃月刀：你使用的杀被闪抵消后，可以对同一目标再使用一张杀。
 */
public class QingLongYanYueDao extends Weapon {

    public QingLongYanYueDao(String id, Suit suit, int number) {
        super(id, "青龙偃月刀", "你使用的杀被闪抵消后，可以对同一目标再使用一张杀", suit, number, 1);
    }
}
