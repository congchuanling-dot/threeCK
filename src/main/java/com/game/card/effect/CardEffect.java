package com.game.card.effect;

import com.game.card.CardContext;
import com.game.card.CardTarget;

/**
 * 卡牌效果接口。
 * 不同牌实现不同效果，便于扩展。
 */
@FunctionalInterface
public interface CardEffect {
    /**
     * 执行卡牌效果
     * @param source 使用者
     * @param target 目标（可为 null，如无中生有无目标）
     * @param context 游戏上下文
     */
    void apply(CardTarget source, CardTarget target, CardContext context);
}
