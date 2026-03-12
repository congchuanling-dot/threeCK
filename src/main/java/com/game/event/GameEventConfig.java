package com.game.event;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 事件驱动配置：将容器内所有 {@link GameEventListener} 注册到 {@link GameEventPublisher}。
 */
@Configuration
public class GameEventConfig {

    @Bean
    public GameEventPublisher gameEventPublisher(List<GameEventListener<?>> listeners) {
        GameEventPublisher publisher = new GameEventPublisher();
        listeners.forEach(publisher::registerListener);
        return publisher;
    }
}
