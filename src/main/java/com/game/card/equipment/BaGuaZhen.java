package com.game.card.equipment;

import com.game.domain.Suit;

/** 八卦阵：当你需要使用或打出闪时，可以进行一次判定，若为红色则视为打出闪 */
public class BaGuaZhen extends Armor {
    public BaGuaZhen(String id, Suit suit, int number) {
        super(id, "八卦阵", "当你需要使用或打出闪时，可以进行一次判定，若为红色则视为打出闪", suit, number);
    }
}
