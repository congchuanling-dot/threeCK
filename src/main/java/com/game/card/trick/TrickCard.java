package com.game.card.trick;

import com.game.card.Card;
import com.game.card.CardType;
import com.game.domain.Suit;

/**
 * 锦囊牌抽象基类。
 */
public abstract class TrickCard extends Card {

    protected TrickCard(String id, String name, String description, Suit suit, int number) {
        super(id, name, description, CardType.TRICK, suit, number);
    }
}
