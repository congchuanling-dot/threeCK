package com.game.card.equipment;

import com.game.domain.Suit;

/** 仁王盾：黑色杀对你无效 */
public class RenWangDun extends Armor {
    public RenWangDun(String id, Suit suit, int number) {
        super(id, "仁王盾", "黑色杀对你无效", suit, number);
    }
}
