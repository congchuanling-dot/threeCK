package com.game.card.trick;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardTarget;
import com.game.domain.Suit;

import java.util.List;

/**
 * 铁索连环：令一名或两名角色进入或解除铁索状态；也可以重铸（弃置摸一张）。
 * （简化：若指定 target 则切换其铁索状态；否则 source 重铸此牌）
 */
public class TieSuoLianHuan extends TrickCard {

    public TieSuoLianHuan(String id, Suit suit, int number) {
        super(id, "铁索连环", "令一名或两名角色进入或解除铁索状态；也可以重铸（弃置摸一张）", suit, number);
    }

    @Override
    public void use(CardTarget source, CardTarget target, CardContext context) {
        if (target != null) {
            target.setChained(!target.isChained());
        } else {
            // 重铸：弃置此牌，摸一张
            source.removeHandCard(this);
            context.addToDiscardPile(this);
            context.drawFromPile().ifPresent(source::addHandCard);
        }
    }
}
