package com.game.card.trick;

import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.domain.Suit;

/**
 * 无懈可击：抵消一张锦囊牌的效果。
 * 使用时机由游戏流程决定，打出后即生效。
 */
public class WuXieKeJi extends TrickCard {

    public WuXieKeJi(String id, Suit suit, int number) {
        super(id, "无懈可击", "抵消一张锦囊牌的效果", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        // 抵消逻辑由游戏流程处理（如清除待生效的锦囊）
    }
}
