package com.game.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 游戏事件发布器。
 * 维护监听器列表，发布事件时通知所有支持该事件类型的监听器。
 * 由 {@link GameEventConfig} 创建并注册所有监听器。
 */
public class GameEventPublisher {

    private final List<GameEventListener<?>> listeners = new CopyOnWriteArrayList<>();

    /** 注册监听器 */
    @SuppressWarnings("unchecked")
    public void registerListener(GameEventListener<?> listener) {
        listeners.add(listener);
    }

    /** 移除监听器 */
    public void removeListener(GameEventListener<?> listener) {
        listeners.remove(listener);
    }

    /**
     * 发布事件，通知所有支持该 eventType 的监听器。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void publish(GameEvent event) {
        String type = event.getEventType();
        for (GameEventListener<?> listener : listeners) {
            if (type.equals(listener.getSupportedEventType())) {
                try {
                    ((GameEventListener) listener).onEvent(event);
                } catch (Exception e) {
                    // 避免单个监听器异常影响其他监听器，可改为记录日志
                    if (Thread.currentThread().getUncaughtExceptionHandler() != null) {
                        Thread.currentThread().getUncaughtExceptionHandler()
                                .uncaughtException(Thread.currentThread(), e);
                    }
                }
            }
        }
    }
}
