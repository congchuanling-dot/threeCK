package com.game.engine;

import com.game.domain.Card;
import com.game.domain.Phase;
import com.game.domain.Player;
import com.game.domain.Suit;
import com.game.service.BotService;
import com.game.skill.GeneralRegistry;
import com.game.event.GameEventPublisher;
import com.game.event.PhaseChangeEvent;
import com.game.service.RoomService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认开局逻辑：52 张基础牌（杀、闪、桃），洗牌，每人发 4 张，当前操作者设为第一人，阶段设为 DRAW。
 * 进入 DRAW 后的自动摸 2 张及流转到 PLAY 由 {@link GameStateMachine#processDrawPhase(int)} 在外部调用。
 */
@Component
public class DefaultGameStarter implements RoomService.GameStarter {

    private static final AtomicInteger CARD_ID = new AtomicInteger(0);

    /** 52 张牌构成：杀 30、闪 12、桃 10 */
    private static final int COUNT_SHA = 30;
    private static final int COUNT_SHAN = 12;
    private static final int COUNT_TAO = 10;

    private final GameEventPublisher eventPublisher;

    public DefaultGameStarter(GameEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void initialize(GameContext context, GameStateMachine stateMachine) {
        List<Card> deck = buildBaseDeck();
        Collections.shuffle(deck);
        context.setDrawPile(deck);

        int handSize = 4;
        for (Player p : context.getRoom().getPlayers()) {
            List<Card> hand = context.drawFromPile(handSize);
            p.addHandCards(hand);
            // 分配武将：房主用选将框选的，机器人默认
            String ownerGeneralId = context.getAttribute("ownerGeneralId").map(Object::toString).orElse("zhaoyun");
            p.setGeneralId(BotService.isBot(p.getPlayerId()) ? "default" : ownerGeneralId);
        }

        context.setCurrentSeatIndex(0);
        context.setCurrentPhase(Phase.DRAW);
        context.setDrawnThisTurn(false);

        stateMachine.setOnTransition(transition -> {
            if (eventPublisher != null) {
                eventPublisher.publish(new PhaseChangeEvent(context, transition));
            }
        });
    }

    /** 52 张基础牌：杀、闪、桃，用不同花色区分 */
    private static List<Card> buildBaseDeck() {
        List<Card> deck = new ArrayList<>();
        Suit[] suits = Suit.values();
        int idx = 0;
        for (int i = 0; i < COUNT_SHA; i++) {
            deck.add(new Card("C" + CARD_ID.incrementAndGet(), suits[idx % suits.length], "杀"));
            idx++;
        }
        for (int i = 0; i < COUNT_SHAN; i++) {
            deck.add(new Card("C" + CARD_ID.incrementAndGet(), suits[idx % suits.length], "闪"));
            idx++;
        }
        for (int i = 0; i < COUNT_TAO; i++) {
            deck.add(new Card("C" + CARD_ID.incrementAndGet(), suits[idx % suits.length], "桃"));
            idx++;
        }
        return deck;
    }
}
