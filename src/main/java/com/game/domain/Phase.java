package com.game.domain;

/**
 * 回合阶段枚举。
 * 控制单回合内的流程：准备 → 摸牌 → 出牌 → 弃牌 → 结束。
 */
public enum Phase {
    /** 准备阶段：回合开始，可执行回合开始技能等 */
    PREPARE("准备"),
    /** 摸牌阶段：从牌堆摸牌 */
    DRAW("摸牌"),
    /** 出牌阶段：打出/使用手牌 */
    PLAY("出牌"),
    /** 弃牌阶段：手牌超过体力上限则弃至上限 */
    DISCARD("弃牌"),
    /** 结束阶段：回合结束，切换到下一玩家 */
    END("结束");

    private final String displayName;

    Phase(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
