package com.game.event;

/**
 * 游戏事件监听器接口。
 * 实现类可监听特定类型的 {@link GameEvent}，在 {@link GameEventPublisher} 发布时被调用。
 *
 * @param <E> 要监听的事件类型
 */
public interface GameEventListener<E extends GameEvent> {

    /**
     * 支持的事件类型，与 {@link GameEvent#getEventType()} 对应。
     */
    String getSupportedEventType();

    /**
     * 处理事件。
     *
     * @param event 事件对象
     */
    void onEvent(E event);
}
