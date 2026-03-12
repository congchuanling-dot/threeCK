package com.game.engine;

import com.game.domain.Card;
import com.game.domain.Phase;
import com.game.domain.Player;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 简易回合阶段状态机。
 * 负责阶段流转：PREPARE → DRAW → PLAY → DISCARD → END，并在 END 时切换当前玩家。
 */
public class GameStateMachine {

    private final GameContext context;
    /** 阶段切换时的回调（可用于发事件、通知前端等） */
    private Consumer<PhaseTransition> onTransition;

    public GameStateMachine(GameContext context) {
        this.context = context;
    }

    /** 设置阶段切换回调 */
    public void setOnTransition(Consumer<PhaseTransition> onTransition) {
        this.onTransition = onTransition;
    }

    /** 当前阶段 */
    public Phase getCurrentPhase() {
        return context.getCurrentPhase();
    }

    /**
     * 推进到下一阶段。
     * PREPARE→DRAW→PLAY→DISCARD→END→(下一玩家)PREPARE...
     */
    public void advancePhase() {
        Phase from = context.getCurrentPhase();
        Phase to = nextPhase(from);
        int fromSeat = context.getCurrentSeatIndex();
        int toSeat = fromSeat;

        if (to == null) {
            // 结束阶段后：切换到下一玩家，并进入准备阶段
            toSeat = context.nextAliveSeatIndex();
            context.setCurrentSeatIndex(toSeat);
            to = Phase.PREPARE;
            context.setDrawnThisTurn(false);
            context.incrementRound();
        } else if (to == Phase.DRAW) {
            context.setDrawnThisTurn(false);
        }

        context.setCurrentPhase(to);
        PhaseTransition transition = new PhaseTransition(from, to, fromSeat, toSeat);
        if (onTransition != null) {
            onTransition.accept(transition);
        }
    }

    /** 进入下一阶段（不切换玩家） */
    private static Phase nextPhase(Phase current) {
        return switch (current) {
            case PREPARE -> Phase.DRAW;
            case DRAW -> Phase.PLAY;
            case PLAY -> Phase.DISCARD;
            case DISCARD -> Phase.END;
            case END -> null; // 由调用方切换玩家并回到 PREPARE
        };
    }

    /** 是否可以在当前阶段出牌 */
    public boolean canPlayCard() {
        return context.getCurrentPhase() == Phase.PLAY;
    }

    /** 是否可以在当前阶段弃牌 */
    public boolean canDiscard() {
        return context.getCurrentPhase() == Phase.DISCARD;
    }

    /** 获取当前行动玩家 */
    public Optional<Player> getCurrentPlayer() {
        return context.getCurrentPlayer();
    }

    /**
     * 摸牌阶段逻辑：给当前玩家摸 count 张牌，并将阶段设为 PLAY（出牌阶段）。
     * 进入 DRAW 时调用一次即可实现“自动摸 2 张并进入出牌阶段”。
     */
    public List<Card> processDrawPhase(int count) {
        if (context.getCurrentPhase() != Phase.DRAW) {
            return List.of();
        }
        Optional<Player> cur = context.getCurrentPlayer();
        if (cur.isEmpty()) return List.of();
        List<Card> drawn = context.drawFromPile(count);
        cur.get().addHandCards(drawn);
        context.setDrawnThisTurn(true);
        context.setCurrentPhase(Phase.PLAY);
        return drawn;
    }

    /**
     * 阶段切换事件数据。
     */
    public record PhaseTransition(Phase fromPhase, Phase toPhase, int fromSeatIndex, int toSeatIndex) {}
}
