package com.game.card.trick;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.card.effect.DamageEffect;
import com.game.domain.Suit;

import java.util.List;

/**
 * 万箭齐发：所有其他角色依次打出一张闪，否则受到1点伤害。
 */
public class WanJianQiFa extends TrickCard {

    public WanJianQiFa(String id, Suit suit, int number) {
        super(id, "万箭齐发", "所有其他角色依次打出一张闪，否则受到1点伤害", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        List<CardTarget> others = context.getAlivePlayers().stream()
                .filter(p -> !p.getId().equals(source.getId()))
                .toList();
        int startSeat = context.nextAliveSeatIndex(context.getCurrentSeatIndex());
        int playerCount = context.getPlayerCount();
        for (int i = 0; i < others.size(); i++) {
            CardTarget victim = findPlayerAtSeat(context, startSeat, playerCount);
            if (victim != null && !victim.getId().equals(source.getId()) && victim.isAlive()) {
                boolean hasShan = victim.getHandCards().stream()
                        .anyMatch(c -> "闪".equals(c.getName()));
                if (!hasShan) {
                    new DamageEffect(1).apply(source, victim, context);
                }
            }
            startSeat = context.nextAliveSeatIndex(startSeat);
        }
    }

    private CardTarget findPlayerAtSeat(CardContext context, int seat, int maxPlayers) {
        for (CardTarget p : context.getAlivePlayers()) {
            if (p.getSeatIndex() == seat) return p;
        }
        return null;
    }
}
