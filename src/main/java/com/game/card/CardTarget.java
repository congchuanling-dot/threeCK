package com.game.card;

import java.util.List;

/**
 * 卡牌系统的目标抽象（玩家等）。
 * 与 {@link com.game.domain.Player} 解耦，便于测试与扩展。
 */
public interface CardTarget {
    String getId();
    String getNickname();
    int getHp();
    void setHp(int hp);
    int getMaxHp();
    void takeDamage(int damage);
    List<? extends Card> getHandCards();
    void addHandCard(Card card);
    void addHandCards(List<? extends Card> cards);
    boolean removeHandCard(Card card);
    boolean isAlive();
    void setAlive(boolean alive);
    int getSeatIndex();

    /** 装备区：武器 */
    Card getWeapon();
    void setWeapon(Card weapon);

    /** 装备区：防具 */
    Card getArmor();
    void setArmor(Card armor);

    /** 装备区：进攻马（-1） */
    Card getOffensiveHorse();
    void setOffensiveHorse(Card horse);

    /** 装备区：防御马（+1） */
    Card getDefensiveHorse();
    void setDefensiveHorse(Card horse);

    /** 是否铁索状态 */
    boolean isChained();
    void setChained(boolean chained);

    /** 是否濒死 */
    boolean isDying();
    void setDying(boolean dying);
}
