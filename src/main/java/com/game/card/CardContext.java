package com.game.card;

import java.util.List;
import java.util.Optional;

/**
 * 卡牌系统的游戏上下文抽象。
 * 提供牌堆、弃牌堆、玩家列表等。
 */
public interface CardContext {
    /** 当前行动玩家 */
    Optional<CardTarget> getCurrentPlayer();
    /** 所有存活玩家（含当前），按座位顺序 */
    List<CardTarget> getAlivePlayers();
    /** 从牌堆顶摸一张牌 */
    Optional<Card> drawFromPile();
    /** 摸多张牌 */
    List<Card> drawFromPile(int count);
    /** 将牌加入弃牌堆 */
    void addToDiscardPile(Card card);
    /** 将牌加入弃牌堆 */
    void addToDiscardPile(List<? extends Card> cards);
    /** 获取当前玩家座位索引 */
    int getCurrentSeatIndex();
    /** 下一存活玩家座位索引 */
    int nextAliveSeatIndex(int fromSeat);
    /** 玩家总数 */
    int getPlayerCount();
}
