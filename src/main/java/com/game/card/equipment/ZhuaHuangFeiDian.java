package com.game.card.equipment;

import com.game.domain.Suit;

/** 爪黄飞电：防御马，其他角色计算与你的距离 +1 */
public class ZhuaHuangFeiDian extends Horse {
    public ZhuaHuangFeiDian(String id, Suit suit, int number) {
        super(id, "爪黄飞电", "其他角色计算与你的距离 +1", suit, number, false);
    }
}
