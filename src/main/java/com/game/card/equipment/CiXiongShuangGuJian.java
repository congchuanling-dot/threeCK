package com.game.card.equipment;

import com.game.domain.Suit;

/**
 * 雌雄双股剑：你使用杀指定异性角色为目标时，其需弃一张牌，否则你摸一张牌。
 */
public class CiXiongShuangGuJian extends Weapon {

    public CiXiongShuangGuJian(String id, Suit suit, int number) {
        super(id, "雌雄双股剑", "你使用杀指定异性角色为目标时，其需弃一张牌，否则你摸一张牌", suit, number, 1);
    }
}
