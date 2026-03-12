package com.game.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 玩家实体。
 * 持有手牌、体力等，与房间内座位/顺序关联。
 */
public class Player {

    private final String playerId;
    private final String nickname;
    /** 座位序号，从 0 开始，用于确定行动顺序 */
    private final int seatIndex;
    /** 当前体力值 */
    private int hp;
    /** 体力上限 */
    private final int maxHp;
    /** 手牌（线程安全） */
    private final List<Card> handCards = new CopyOnWriteArrayList<>();
    /** 是否已阵亡/离开 */
    private boolean alive = true;

    public Player(String playerId, String nickname, int seatIndex, int maxHp) {
        this.playerId = Objects.requireNonNull(playerId, "playerId must not be null");
        this.nickname = nickname != null ? nickname : playerId;
        this.seatIndex = seatIndex;
        this.maxHp = Math.max(1, maxHp);
        this.hp = this.maxHp;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getNickname() {
        return nickname;
    }

    public int getSeatIndex() {
        return seatIndex;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = Math.max(0, Math.min(hp, maxHp));
        if (this.hp <= 0) {
            this.alive = false;
        }
    }

    /** 受到伤害时调用，扣减体力 */
    public void takeDamage(int damage) {
        setHp(this.hp - Math.max(0, damage));
    }

    public int getMaxHp() {
        return maxHp;
    }

    /** 手牌数量上限，通常等于当前体力值 */
    public int getHandLimit() {
        return hp;
    }

    public List<Card> getHandCards() {
        return Collections.unmodifiableList(handCards);
    }

    /** 加入手牌 */
    public void addHandCard(Card card) {
        handCards.add(card);
    }

    /** 加入多张手牌 */
    public void addHandCards(List<Card> cards) {
        handCards.addAll(cards);
    }

    /** 从手牌中移除指定牌（出牌/弃牌时调用） */
    public boolean removeHandCard(Card card) {
        return handCards.remove(card);
    }

    /** 根据 id 移除手牌 */
    public boolean removeHandCardById(String cardId) {
        return handCards.removeIf(c -> c.getId().equals(cardId));
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
