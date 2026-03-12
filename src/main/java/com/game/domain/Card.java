package com.game.domain;

import java.util.Objects;

/**
 * 卡牌实体。
 * 包含花色、点数/名称，可扩展为技能牌、装备牌等。
 */
public class Card {

    private final String id;
    private final Suit suit;
    /** 点数或牌名，如 "A","2"..."K" 或 "杀","闪" 等 */
    private final String rankOrName;

    public Card(String id, Suit suit, String rankOrName) {
        this.id = Objects.requireNonNull(id, "card id must not be null");
        this.suit = Objects.requireNonNull(suit, "suit must not be null");
        this.rankOrName = Objects.requireNonNull(rankOrName, "rankOrName must not be null");
    }

    public String getId() {
        return id;
    }

    public Suit getSuit() {
        return suit;
    }

    public String getRankOrName() {
        return rankOrName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return id.equals(card.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return suit.getSymbol() + rankOrName + "(" + id + ")";
    }
}
