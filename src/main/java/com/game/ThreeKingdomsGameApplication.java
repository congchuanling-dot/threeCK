package com.game;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 三国杀风格回合制卡牌游戏 - 后端启动类。
 * 基于 Spring Boot 3 + Netty（WebFlux）运行。
 */
@SpringBootApplication
public class ThreeKingdomsGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThreeKingdomsGameApplication.class, args);
    }
}
