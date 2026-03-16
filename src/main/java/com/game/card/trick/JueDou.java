package com.game.card.trick;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.card.effect.DamageEffect;
import com.game.domain.Suit;

import java.util.List;

/**
 * 决斗：目标角色与你轮流打出杀，先无法打出杀的一方受到1点伤害。
 * （简化：双方轮流从手牌打出一张杀，先无杀者受伤）
 */
public class JueDou extends TrickCard {

    public JueDou(String id, Suit suit, int number) {
        super(id, "决斗", "目标角色与你轮流打出杀，先无法打出杀的一方受到1点伤害", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        if (target == null || !target.isAlive()) return;
        CardTarget loser = duel(source, target, context);
        if (loser != null) {
            new DamageEffect(1).apply(source, loser, context);
        }
    }

    private CardTarget duel(CardTarget source, CardTarget target, CardContext context) {
        CardTarget current = source;
        CardTarget other = target;
        while (true) {
            Card sha = findSha(current.getHandCards());
            if (sha == null) return current;
            current.removeHandCard(sha);
            context.addToDiscardPile(sha);
            CardTarget tmp = current;
            current = other;
            other = tmp;
        }
    }

    private Card findSha(List<? extends Card> hand) {
        for (Card c : hand) {
            if ("杀".equals(c.getName())) return c;
        }
        return null;
    }
}
