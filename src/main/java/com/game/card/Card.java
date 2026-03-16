package com.game.card;

import com.game.card.effect.CardEffect;
import com.game.domain.Suit;

/**
 * 卡牌抽象基类。
 * 所有牌（基础牌、锦囊牌、装备牌）的父类，使用策略模式委托效果执行。
 */
public abstract class Card {

    protected final String id;
    protected final String name;
    protected final String description;
    protected final CardType cardType;
    protected final Suit suit;
    protected final int number;

    protected Card(String id, String name, String description, CardType cardType, Suit suit, int number) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cardType = cardType;
        this.suit = suit;
        this.number = Math.max(0, Math.min(number, 13));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public CardType getCardType() {
        return cardType;
    }

    public Suit getSuit() {
        return suit;
    }

    public int getNumber() {
        return number;
    }

    /**
     * 使用此牌。由子类或效果实现具体逻辑。
     */
    public abstract void use(CardTarget source, CardTarget target, CardContext context);

    /**
     * 获取牌的效果（用于策略模式，部分牌通过组合效果实现）
     */
    protected CardEffect getEffect() {
        return null;
    }

    /**
     * 转换为 domain.Card，用于与现有引擎集成（手牌、牌堆存储）。
     */
    public com.game.domain.Card toDomainCard() {
        return new com.game.domain.Card(id, suit, name);
    }

    @Override
    public String toString() {
        return suit.getSymbol() + name + "(" + id + ")";
    }
}
