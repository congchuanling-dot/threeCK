package com.game.card.impl;

import com.game.card.Card;
import com.game.card.CardTarget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CardTarget 的完整实现，用于卡牌系统 standalone 模式。
 * 持有手牌、装备、体力等，与 domain.Player 解耦。
 */
public class CardTargetImpl implements CardTarget {

    private final String id;
    private final String nickname;
    private final int seatIndex;
    private int hp;
    private final int maxHp;
    private final List<Card> handCards = new ArrayList<>();
    private boolean alive = true;

    private Card weapon;
    private Card armor;
    private Card offensiveHorse;
    private Card defensiveHorse;
    private boolean chained;
    private boolean dying;

    public CardTargetImpl(String id, String nickname, int seatIndex, int maxHp) {
        this.id = id;
        this.nickname = nickname != null ? nickname : id;
        this.seatIndex = seatIndex;
        this.maxHp = Math.max(1, maxHp);
        this.hp = this.maxHp;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    @Override
    public int getHp() {
        return hp;
    }

    @Override
    public void setHp(int hp) {
        this.hp = Math.max(0, Math.min(hp, maxHp));
        if (this.hp <= 0) {
            this.alive = false;
            this.dying = true;
        }
    }

    @Override
    public void takeDamage(int damage) {
        setHp(this.hp - Math.max(0, damage));
    }

    @Override
    public int getMaxHp() {
        return maxHp;
    }

    @Override
    public List<? extends Card> getHandCards() {
        return Collections.unmodifiableList(handCards);
    }

    @Override
    public void addHandCard(Card card) {
        if (card != null) handCards.add(card);
    }

    @Override
    public void addHandCards(List<? extends Card> cards) {
        if (cards != null) handCards.addAll(cards);
    }

    @Override
    public boolean removeHandCard(Card card) {
        return handCards.remove(card);
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Override
    public int getSeatIndex() {
        return seatIndex;
    }

    @Override
    public Card getWeapon() {
        return weapon;
    }

    @Override
    public void setWeapon(Card weapon) {
        this.weapon = weapon;
    }

    @Override
    public Card getArmor() {
        return armor;
    }

    @Override
    public void setArmor(Card armor) {
        this.armor = armor;
    }

    @Override
    public Card getOffensiveHorse() {
        return offensiveHorse;
    }

    @Override
    public void setOffensiveHorse(Card horse) {
        this.offensiveHorse = horse;
    }

    @Override
    public Card getDefensiveHorse() {
        return defensiveHorse;
    }

    @Override
    public void setDefensiveHorse(Card horse) {
        this.defensiveHorse = horse;
    }

    @Override
    public boolean isChained() {
        return chained;
    }

    @Override
    public void setChained(boolean chained) {
        this.chained = chained;
    }

    @Override
    public boolean isDying() {
        return dying;
    }

    @Override
    public void setDying(boolean dying) {
        this.dying = dying;
    }
}
