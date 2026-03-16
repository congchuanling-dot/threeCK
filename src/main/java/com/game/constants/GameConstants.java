package com.game.constants;

/**
 * 游戏全局常量，便于配置与扩展。
 */
public final class GameConstants {
    /** 初始体力 */
    public static final int INITIAL_HP = 4;
    /** 每次摸牌数量 */
    public static final int DRAW_COUNT_PER_TURN = 2;
    /** 初始手牌数量 */
    public static final int INITIAL_HAND_COUNT = 4;
    /** 默认房主武将ID（选将未选时使用） */
    public static final String DEFAULT_OWNER_GENERAL_ID = "zhaoyun";
    /** 龙胆技能ID */
    public static final String SKILL_LONGDAN = "longdan";

    private GameConstants() {}
}
