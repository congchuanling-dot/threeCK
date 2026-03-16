package com.game.card.basic;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.card.CardType;
import com.game.domain.Suit;

/**
 * 基础牌抽象基类。
 */
public abstract class BasicCard extends Card {

    protected BasicCard(String id, String name, String description, Suit suit, int number) {
        super(id, name, description, CardType.BASIC, suit, number);
    }
}
