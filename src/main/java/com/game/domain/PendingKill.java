package com.game.domain;

/**
 * 待响应的「杀」：目标玩家可出「闪」抵消，否则受到伤害。
 */
public record PendingKill(String targetId, String targetName, String sourceId, String sourceName, int amount) {}
