package com.game.card.equipment;

import com.game.domain.Suit;

/**
 * 诸葛连弩：你在出牌阶段可以使用任意数量的杀。
 * （效果由游戏流程在出牌阶段检查）
 */
public class ZhuGeLianNu extends Weapon {

    public ZhuGeLianNu(String id, Suit suit, int number) {
        super(id, "诸葛连弩", "你在出牌阶段可以使用任意数量的杀", suit, number, 1);
    }
}
