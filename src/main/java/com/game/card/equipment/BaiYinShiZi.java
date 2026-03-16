package com.game.card.equipment;

import com.game.domain.Suit;

/** 白银狮子：当你受到伤害时，伤害最多为1；失去装备时回复1点体力 */
public class BaiYinShiZi extends Armor {
    public BaiYinShiZi(String id, Suit suit, int number) {
        super(id, "白银狮子", "当你受到伤害时，伤害最多为1；失去装备时回复1点体力", suit, number);
    }
}
