package com.game.card.example;

import com.game.card.Card;
import com.game.card.CardContext;
import com.game.card.CardFactory;
import com.game.card.CardTarget;
import com.game.card.impl.CardContextImpl;
import com.game.card.impl.CardTargetImpl;
import com.game.card.trick.NanManRuQin;
import com.game.domain.Suit;

import java.util.ArrayList;
import java.util.List;

/**
 * 示例：玩家使用南蛮入侵的执行流程。
 *
 * 流程说明：
 * 1. 当前玩家（source）使用南蛮入侵
 * 2. 从 source 的下家开始，按顺时针依次结算
 * 3. 每位其他角色需要打出一张「杀」，否则受到 1 点伤害
 * 4. 简化实现：若手牌中有杀则视为打出，无杀则直接受伤
 */
public class NanManRuQinExample {

    public static void main(String[] args) {
        // 1. 创建 4 名玩家
        List<CardTarget> players = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            players.add(new CardTargetImpl("P" + i, "玩家" + (i + 1), i, 4));
        }

        // 2. 构建牌堆（含南蛮入侵、杀等）
        List<Card> drawPile = new ArrayList<>();
        drawPile.add(CardFactory.create("南蛮入侵", Suit.SPADE, 7).orElseThrow());
        drawPile.add(CardFactory.create("杀", Suit.SPADE, 8).orElseThrow());
        drawPile.add(CardFactory.create("杀", Suit.HEART, 9).orElseThrow());
        // 玩家 1、2 无杀，玩家 3 有杀
        for (int i = 0; i < 10; i++) {
            CardFactory.create("桃", Suit.HEART, 1).ifPresent(drawPile::add);
        }

        // 3. 发手牌：玩家 1（当前）有南蛮入侵，玩家 2、3 无杀，玩家 4 有杀
        players.get(0).addHandCard(CardFactory.create("南蛮入侵", Suit.SPADE, 7).orElseThrow());
        players.get(1).addHandCards(List.of(
                CardFactory.create("桃", Suit.HEART, 1).orElseThrow(),
                CardFactory.create("桃", Suit.HEART, 1).orElseThrow()));
        players.get(2).addHandCards(List.of(
                CardFactory.create("桃", Suit.HEART, 1).orElseThrow()));
        players.get(3).addHandCard(CardFactory.create("杀", Suit.SPADE, 8).orElseThrow());

        // 4. 创建游戏上下文
        CardContextImpl context = new CardContextImpl(players, drawPile);
        context.setCurrentSeatIndex(0); // 玩家 0 当前回合

        // 5. 执行：玩家 0 使用南蛮入侵
        CardTarget source = players.get(0);
        Card nanMan = source.getHandCards().stream()
                .filter(c -> "南蛮入侵".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        source.removeHandCard(nanMan);
        context.addToDiscardPile(nanMan);

        nanMan.use(source, null, context);

        // 6. 输出结果
        System.out.println("=== 南蛮入侵执行完毕 ===");
        for (CardTarget p : players) {
            String status = p.isAlive() ? "存活" : "阵亡";
            System.out.printf("%s: HP=%d/%d, 手牌数=%d [%s]%n",
                    p.getNickname(), p.getHp(), p.getMaxHp(),
                    p.getHandCards().size(), status);
        }
        // 预期：玩家 2、3（座位 1、2）因无杀各受到 1 点伤害；玩家 4（座位 3）有杀，不受伤
    }
}
