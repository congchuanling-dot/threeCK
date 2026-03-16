package com.game.card.adapter;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardFactory;
import com.game.card.CardTarget;
import com.game.domain.Player;
import com.game.engine.GameContext;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 将 engine.GameContext 适配为 CardContext，使卡牌系统可操作游戏上下文。
 * 牌堆中的 domain.Card 会在摸牌时转换为 card.Card；弃牌时转换回 domain.Card。
 */
public class GameContextCardContextAdapter implements CardContext {

    private final GameContext ctx;

    public GameContextCardContextAdapter(GameContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Optional<CardTarget> getCurrentPlayer() {
        return ctx.getCurrentPlayer()
                .map(PlayerCardTargetAdapter::new)
                .map(p -> (CardTarget) p);
    }

    @Override
    public List<CardTarget> getAlivePlayers() {
        return ctx.getRoom().getPlayers().stream()
                .filter(Player::isAlive)
                .sorted((a, b) -> Integer.compare(a.getSeatIndex(), b.getSeatIndex()))
                .map(PlayerCardTargetAdapter::new)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Card> drawFromPile() {
        return ctx.drawFromPile()
                .flatMap(CardFactory::createFromDomainCard);
    }

    @Override
    public List<Card> drawFromPile(int count) {
        List<com.game.domain.Card> drawn = ctx.drawFromPile(count);
        return drawn.stream()
                .flatMap(dc -> CardFactory.createFromDomainCard(dc).stream())
                .collect(Collectors.toList());
    }

    @Override
    public void addToDiscardPile(Card card) {
        if (card != null) {
            ctx.addToDiscardPile(card.toDomainCard());
        }
    }

    @Override
    public void addToDiscardPile(List<? extends Card> cards) {
        if (cards != null) {
            for (Card c : cards) {
                addToDiscardPile(c);
            }
        }
    }

    @Override
    public int getCurrentSeatIndex() {
        return ctx.getCurrentSeatIndex();
    }

    @Override
    public int nextAliveSeatIndex(int fromSeat) {
        int n = ctx.getRoom().getMaxPlayers();
        for (int i = 1; i <= n; i++) {
            int next = ((fromSeat + i) % n + n) % n;
            Optional<Player> p = ctx.getRoom().getPlayerBySeat(next);
            if (p.isPresent() && p.get().isAlive()) return next;
        }
        return fromSeat;
    }

    @Override
    public int getPlayerCount() {
        return ctx.getRoom().getMaxPlayers();
    }

    public GameContext getGameContext() {
        return ctx;
    }
}
