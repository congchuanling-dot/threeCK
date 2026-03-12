package com.game.event;

import com.game.engine.GameContext;

/**
 * 游戏事件标记接口。
 * 所有游戏内事件（出牌、受到伤害、阶段切换等）可继承此接口，便于统一发布与监听。
 */
public interface GameEvent {

    /**
     * 事件所属的当局游戏上下文。
     */
    GameContext getContext();

    /**
     * 事件类型标识，用于监听器筛选。
     */
    String getEventType();
}
