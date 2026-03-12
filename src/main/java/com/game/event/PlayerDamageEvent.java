package com.game.event;

import com.game.domain.Player;
import com.game.engine.GameContext;

/**
 * 玩家受到伤害事件。
 * 在结算伤害时发布，供技能、濒死等监听器处理。
 */
public class PlayerDamageEvent implements GameEvent {

    public static final String EVENT_TYPE = "PLAYER_DAMAGE";

    private final GameContext context;
    /** 受到伤害的玩家 */
    private final Player target;
    /** 伤害来源（可为 null，如体力流失） */
    private final Player source;
    private final int amount;

    public PlayerDamageEvent(GameContext context, Player target, Player source, int amount) {
        this.context = context;
        this.target = target;
        this.source = source;
        this.amount = Math.max(0, amount);
    }

    @Override
    public GameContext getContext() {
        return context;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    public Player getTarget() {
        return target;
    }

    public Player getSource() {
        return source;
    }

    public int getAmount() {
        return amount;
    }
}
