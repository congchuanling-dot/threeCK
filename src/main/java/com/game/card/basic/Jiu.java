package com.game.card.basic;

import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.card.effect.HealEffect;
import com.game.domain.Suit;

/**
 * 酒：本回合下一张杀伤害+1；若角色处于濒死状态，可以当桃使用。
 * （简化实现：若目标濒死则当桃，否则标记「本回合下一张杀+1」由流程处理）
 */
public class Jiu extends BasicCard {

    public Jiu(String id, Suit suit, int number) {
        super(id, "酒", "本回合下一张杀伤害+1；若角色处于濒死状态，可以当桃使用", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        CardTarget recipient = target != null ? target : source;
        if (recipient != null && recipient.isDying()) {
            new HealEffect(1).apply(source, recipient, context);
        }
        // 非濒死时，标记由 context.setAttribute("jiuBuff", true) 等方式处理，此处省略
    }
}
