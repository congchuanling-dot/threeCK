package com.game.card.basic;

import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.domain.Suit;

/**
 * 闪：抵消一张杀。
 * 使用时机由游戏流程决定，打出后即生效（抵消待处理的杀）。
 */
public class Shan extends BasicCard {

    public Shan(String id, Suit suit, int number) {
        super(id, "闪", "抵消一张杀", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        // 闪的效果在打出时由游戏流程处理（清除 pendingKill 等），此处无直接目标
    }
}
