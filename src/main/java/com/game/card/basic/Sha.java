package com.game.card.basic;

import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.card.effect.DamageEffect;
import com.game.domain.Suit;

/**
 * 杀：对一名目标角色造成1点伤害，目标可以打出一张闪来抵消。
 * （简化实现：直接造成伤害，闪的抵消由游戏流程处理）
 */
public class Sha extends BasicCard {

    public Sha(String id, Suit suit, int number) {
        super(id, "杀", "对一名目标角色造成1点伤害，目标可以打出一张闪来抵消", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        if (target != null && target.isAlive()) {
            new DamageEffect(1).apply(source, target, context);
        }
    }
}
