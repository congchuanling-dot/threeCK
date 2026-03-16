package com.game.card.trick;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.domain.Suit;

import java.util.List;

/**
 * 借刀杀人：指定一名有武器的角色对另一名角色使用一张杀，否则你获得该角色的武器。
 * （简化：若目标有武器且不愿出杀，则武器转移到 source）
 */
public class JieDaoShaRen extends TrickCard {

    public JieDaoShaRen(String id, Suit suit, int number) {
        super(id, "借刀杀人", "指定一名有武器的角色对另一名角色使用一张杀，否则你获得该角色的武器", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        if (target == null) return;
        Card weapon = target.getWeapon();
        if (weapon == null) return;
        // 简化：假设目标未出杀，source 获得武器
        target.setWeapon(null);
        source.addHandCard(weapon);
    }
}
