package com.game.card.equipment;

import com.game.domain.Suit;

/** 藤甲：普通杀和万箭齐发对你无效，但你受到火焰伤害+1 */
public class TengJia extends Armor {
    public TengJia(String id, Suit suit, int number) {
        super(id, "藤甲", "普通杀和万箭齐发对你无效，但你受到火焰伤害+1", suit, number);
    }
}
