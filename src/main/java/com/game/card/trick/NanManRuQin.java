package com.game.card.trick;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.card.effect.DamageEffect;
import com.game.domain.Suit;

import java.util.List;

/**
 * 南蛮入侵：所有其他角色按顺时针依次打出一张杀，否则受到1点伤害。
 * （简化实现：未打出杀则直接受到伤害，实际游戏中需等待玩家响应）
 */
public class NanManRuQin extends TrickCard {

    public NanManRuQin(String id, Suit suit, int number) {
        super(id, "南蛮入侵", "所有其他角色按顺时针依次打出一张杀，否则受到1点伤害", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        List<CardTarget> others = context.getAlivePlayers().stream()
                .filter(p -> !p.getId().equals(source.getId()))
                .toList();
        int seat = context.nextAliveSeatIndex(context.getCurrentSeatIndex());
        for (int i = 0; i < others.size(); i++) {
            final int seatToCheck = seat;
            CardTarget victim = others.stream()
                    .filter(p -> p.getSeatIndex() == seatToCheck)
                    .findFirst()
                    .orElse(null);
            if (victim != null && victim.isAlive()) {
                boolean hasSha = victim.getHandCards().stream()
                        .anyMatch(c -> "杀".equals(c.getName()));
                if (!hasSha) {
                    new DamageEffect(1).apply(source, victim, context);
                }
            }
            seat = context.nextAliveSeatIndex(seat);
        }
    }
}
