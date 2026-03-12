package com.game.event;

import com.game.domain.Phase;
import com.game.engine.GameContext;
import com.game.engine.GameStateMachine;

/**
 * 阶段切换事件。
 * 状态机推进阶段时发布，便于广播、日志或技能触发。
 */
public class PhaseChangeEvent implements GameEvent {

    public static final String EVENT_TYPE = "PHASE_CHANGE";

    private final GameContext context;
    private final GameStateMachine.PhaseTransition transition;

    public PhaseChangeEvent(GameContext context, GameStateMachine.PhaseTransition transition) {
        this.context = context;
        this.transition = transition;
    }

    @Override
    public GameContext getContext() {
        return context;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    public GameStateMachine.PhaseTransition getTransition() {
        return transition;
    }
}
