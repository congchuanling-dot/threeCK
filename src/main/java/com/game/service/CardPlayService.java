package com.game.service;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardFactory;
import com.game.card.CardTarget;
import com.game.card.CardType;
import com.game.card.adapter.GameContextCardContextAdapter;
import com.game.card.adapter.PlayerCardTargetAdapter;
import com.game.domain.Player;
import com.game.engine.GameContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 卡牌效果执行服务。将卡牌系统的 use() 与游戏引擎桥接。
 */
@Service
public class CardPlayService {

    /**
     * 执行卡牌效果（锦囊、装备等）。调用前需确保已从手牌移除并加入弃牌堆。
     * @return true 表示成功执行
     */
    public boolean executeCardEffect(GameContext ctx, Player source, com.game.domain.Card domainCard, Player target) {
        Optional<Card> opt = CardFactory.createFromDomainCard(domainCard);
        if (opt.isEmpty()) return false;

        Card card = opt.get();
        CardType type = card.getCardType();

        // 仅处理锦囊和装备，基础牌由 Controller 特殊流程处理
        if (type != CardType.TRICK && type != CardType.EQUIPMENT) {
            return false;
        }

        CardContext cardCtx = new GameContextCardContextAdapter(ctx);
        CardTarget sourceTarget = new PlayerCardTargetAdapter(source);
        CardTarget targetTarget = target != null ? new PlayerCardTargetAdapter(target) : sourceTarget;

        card.use(sourceTarget, targetTarget, cardCtx);
        return true;
    }

    /**
     * 判断牌是否可由 CardPlayService 执行（锦囊/装备）
     */
    public static boolean isHandledByCardSystem(String rankOrName) {
        if (rankOrName == null) return false;
        Optional<Card> opt = CardFactory.create("_", rankOrName, com.game.domain.Suit.SPADE, 1);
        if (opt.isEmpty()) return false;
        CardType t = opt.get().getCardType();
        return t == CardType.TRICK || t == CardType.EQUIPMENT;
    }

    /**
     * 判断牌是否为装备牌（需装备到自己）
     */
    public static boolean isEquipment(String rankOrName) {
        if (rankOrName == null) return false;
        Optional<Card> opt = CardFactory.create("_", rankOrName, com.game.domain.Suit.SPADE, 1);
        return opt.isPresent() && opt.get().getCardType() == CardType.EQUIPMENT;
    }

    /**
     * 判断锦囊是否需要指定目标
     */
    public static boolean trickNeedsTarget(String rankOrName) {
        if (rankOrName == null) return false;
        return switch (rankOrName) {
            case "过河拆桥", "顺手牵羊", "借刀杀人", "决斗", "火攻", "铁索连环" -> true;
            default -> false;
        };
    }
}
