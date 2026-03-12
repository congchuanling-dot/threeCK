package com.game.event;

import com.game.domain.Card;
import com.game.engine.GameContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 默认“玩家出牌”事件监听器示例。
 * 出牌后将牌移入弃牌堆；可在此扩展技能结算、广播等。
 */
@Component
public class DefaultPlayerPlayCardListener implements GameEventListener<PlayerPlayCardEvent> {

    private static final Logger log = LoggerFactory.getLogger(DefaultPlayerPlayCardListener.class);

    @Override
    public String getSupportedEventType() {
        return PlayerPlayCardEvent.EVENT_TYPE;
    }

    @Override
    public void onEvent(PlayerPlayCardEvent event) {
        GameContext ctx = event.getContext();
        Card card = event.getCard();
        if (card != null) {
            ctx.addToDiscardPile(card);
            log.debug("玩家 {} 打出 {}，已加入弃牌堆", event.getPlayer().getPlayerId(), card);
        }
    }
}
