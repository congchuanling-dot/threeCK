package com.game.card.impl;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardTarget;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CardContext 的完整实现，用于卡牌系统 standalone 模式。
 * 持有牌堆、弃牌堆、玩家列表。
 */
public class CardContextImpl implements CardContext {

    private final List<CardTarget> players;
    private final Deque<Card> drawPile;
    private final List<Card> discardPile = new ArrayList<>();
    private int currentSeatIndex;
    private final int playerCount;

    public CardContextImpl(List<CardTarget> players, List<Card> initialDrawPile) {
        this.players = new ArrayList<>(players);
        this.drawPile = new LinkedList<>(initialDrawPile != null ? initialDrawPile : List.of());
        this.playerCount = players.size();
        this.currentSeatIndex = 0;
    }

    @Override
    public Optional<CardTarget> getCurrentPlayer() {
        return players.stream()
                .filter(p -> p.getSeatIndex() == currentSeatIndex)
                .findFirst();
    }

    @Override
    public List<CardTarget> getAlivePlayers() {
        return players.stream()
                .filter(CardTarget::isAlive)
                .sorted(Comparator.comparingInt(CardTarget::getSeatIndex))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Card> drawFromPile() {
        if (drawPile.isEmpty()) return Optional.empty();
        return Optional.of(drawPile.poll());
    }

    @Override
    public List<Card> drawFromPile(int count) {
        List<Card> drawn = new ArrayList<>();
        for (int i = 0; i < count && !drawPile.isEmpty(); i++) {
            drawn.add(drawPile.poll());
        }
        return drawn;
    }

    @Override
    public void addToDiscardPile(Card card) {
        if (card != null) discardPile.add(card);
    }

    @Override
    public void addToDiscardPile(List<? extends Card> cards) {
        if (cards != null) discardPile.addAll(cards);
    }

    @Override
    public int getCurrentSeatIndex() {
        return currentSeatIndex;
    }

    public void setCurrentSeatIndex(int index) {
        this.currentSeatIndex = ((index % playerCount) + playerCount) % playerCount;
    }

    @Override
    public int nextAliveSeatIndex(int fromSeat) {
        for (int i = 1; i <= playerCount; i++) {
            int next = ((fromSeat + i) % playerCount + playerCount) % playerCount;
            int seat = next;
            CardTarget p = players.stream().filter(x -> x.getSeatIndex() == seat && x.isAlive()).findFirst().orElse(null);
            if (p != null) return seat;
        }
        return fromSeat;
    }

    @Override
    public int getPlayerCount() {
        return playerCount;
    }
}
