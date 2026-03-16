package com.game.card.equipment;

import com.game.domain.Suit;

/**
 * 麒麟弓：当你使用杀造成伤害后，可以弃置目标的一匹马。
 */
public class QiLinGong extends Weapon {

    public QiLinGong(String id, Suit suit, int number) {
        super(id, "麒麟弓", "当你使用杀造成伤害后，可以弃置目标的一匹马", suit, number, 1);
    }
}
