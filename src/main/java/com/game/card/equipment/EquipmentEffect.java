package com.game.card.equipment;

import com.game.card.CardContext;
import com.game.card.CardTarget;

/**
 * 装备持续效果接口。
 * 装备牌进入装备区时触发，或在特定时机生效。
 */
public interface EquipmentEffect {
    /**
     * 应用装备效果（如判定、距离修正等）
     * @param player 装备持有者
     * @param context 游戏上下文
     */
    void apply(CardTarget player, CardContext context);
}
