package com.game.event;

import com.game.domain.Card;
import com.game.domain.Player;
import com.game.engine.GameContext;

/**
 * 玩家出牌事件。
 * 在玩家打出一张牌时发布，供技能、结算等监听器处理。
 */
public class PlayerPlayCardEvent implements GameEvent {

    public static final String EVENT_TYPE = "PLAYER_PLAY_CARD";

    private final GameContext context;
    private final Player player;
    private final Card card;
    /** 目标玩家（若有，如决斗、杀等） */
    private final Player targetPlayer;

    public PlayerPlayCardEvent(GameContext context, Player player, Card card, Player targetPlayer) {
        this.context = context;
        this.player = player;
        this.card = card;
        this.targetPlayer = targetPlayer;
    }

    public PlayerPlayCardEvent(GameContext context, Player player, Card card) {
        this(context, player, card, null);
    }

    @Override
    public GameContext getContext() {
        return context;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    public Player getPlayer() {
        return player;
    }

    public Card getCard() {
        return card;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }
}
