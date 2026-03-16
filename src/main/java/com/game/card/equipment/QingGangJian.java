package com.game.card.equipment;

import com.game.domain.Suit;

/**
 * 青釭剑：你使用杀时无视目标的防具。
 */
public class QingGangJian extends Weapon {

    public QingGangJian(String id, Suit suit, int number) {
        super(id, "青釭剑", "你使用杀时无视目标的防具", suit, number, 1);
    }
}
