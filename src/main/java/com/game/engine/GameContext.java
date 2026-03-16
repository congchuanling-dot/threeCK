package com.game.engine;

import com.game.domain.Card;
import com.game.domain.GameRoom;
import com.game.domain.Player;
import com.game.domain.Phase;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 游戏核心上下文。
 * 保存当前牌堆、弃牌堆、当前回合玩家、当前阶段等实时状态，与一个 GameRoom 绑定。
 */
public class GameContext {

    private final GameRoom room;
    /** 牌堆（摸牌从这里取） */
    private final Deque<Card> drawPile = new LinkedList<>();
    /** 弃牌堆（打出的牌进入此处，牌堆空时可洗入） */
    private final List<Card> discardPile = new CopyOnWriteArrayList<>();
    /** 当前回合的玩家座位索引 */
    private volatile int currentSeatIndex = 0;
    /** 当前阶段 */
    private volatile Phase currentPhase = Phase.PREPARE;
    /** 当前回合数（每轮完一圈 +1） */
    private final AtomicInteger roundNumber = new AtomicInteger(1);
    /** 阶段内是否已执行过摸牌（避免重复摸牌） */
    private volatile boolean drawnThisTurn = false;
    /** 扩展数据（可选，用于挂载技能、标记等） */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    /** 最近打出的牌（用于中央出牌区展示），最多保留 8 条 */
    private static final int BATTLE_CARDS_MAX = 8;
    private final List<Map<String, Object>> recentBattleCards = new CopyOnWriteArrayList<>();

    public GameContext(GameRoom room) {
        this.room = Objects.requireNonNull(room, "room must not be null");
    }

    public GameRoom getRoom() {
        return room;
    }

    /** 当前行动玩家 */
    public Optional<Player> getCurrentPlayer() {
        return room.getPlayerBySeat(currentSeatIndex);
    }

    public int getCurrentSeatIndex() {
        return currentSeatIndex;
    }

    public void setCurrentSeatIndex(int currentSeatIndex) {
        this.currentSeatIndex = normalizeSeat(currentSeatIndex);
    }

    /** 下一座位的索引（按座位轮询，不论存活），用于濒死轮询 */
    public int nextSeatIndex(int fromSeat) {
        return normalizeSeat(fromSeat + 1);
    }

    /** 下一顺位存活玩家的座位索引 */
    public int nextAliveSeatIndex() {
        int n = room.getMaxPlayers();
        for (int i = 1; i <= n; i++) {
            int next = normalizeSeat(currentSeatIndex + i);
            Optional<Player> p = room.getPlayerBySeat(next);
            if (p.isPresent() && p.get().isAlive()) return next;
        }
        return currentSeatIndex;
    }

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(Phase phase) {
        this.currentPhase = phase;
    }

    public int getRoundNumber() {
        return roundNumber.get();
    }

    public void incrementRound() {
        roundNumber.incrementAndGet();
    }

    public boolean isDrawnThisTurn() {
        return drawnThisTurn;
    }

    public void setDrawnThisTurn(boolean drawnThisTurn) {
        this.drawnThisTurn = drawnThisTurn;
    }

    /** 牌堆顶摸一张，无牌时先洗入弃牌堆再摸 */
    public Optional<Card> drawFromPile() {
        if (drawPile.isEmpty()) {
            shuffleDiscardIntoDraw();
        }
        return Optional.ofNullable(drawPile.poll());
    }

    /** 摸多张牌 */
    public List<Card> drawFromPile(int count) {
        List<Card> drawn = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            drawFromPile().ifPresent(drawn::add);
        }
        return drawn;
    }

    /** 将弃牌堆洗入牌堆 */
    public void shuffleDiscardIntoDraw() {
        List<Card> list = new ArrayList<>(discardPile);
        discardPile.clear();
        Collections.shuffle(list);
        drawPile.addAll(list);
    }

    /** 将一张牌加入弃牌堆 */
    public void addToDiscardPile(Card card) {
        if (card != null) discardPile.add(card);
    }

    /** 将多张牌加入弃牌堆 */
    public void addToDiscardPile(Collection<Card> cards) {
        if (cards != null) discardPile.addAll(cards);
    }

    /** 初始化牌堆（发牌前调用） */
    public void setDrawPile(List<Card> cards) {
        drawPile.clear();
        if (cards != null) drawPile.addAll(cards);
    }

    /** 牌堆剩余数量 */
    public int getDrawPileSize() {
        return drawPile.size();
    }

    /** 弃牌堆数量 */
    public int getDiscardPileSize() {
        return discardPile.size();
    }

    /** 牌堆的只读视图（LIFO 队列，不修改原牌堆） */
    public Queue<Card> getDrawPileView() {
        return Collections.asLifoQueue(new ArrayDeque<>(drawPile));
    }

    public List<Card> getDiscardPile() {
        return Collections.unmodifiableList(discardPile);
    }

    /** 座位索引规范化 [0, maxPlayers) */
    private int normalizeSeat(int index) {
        int n = room.getMaxPlayers();
        return ((index % n) + n) % n;
    }

    public void setAttribute(String key, Object value) {
        if (key != null) attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAttribute(String key) {
        return Optional.ofNullable((T) attributes.get(key));
    }

    /** 待响应杀的键名 */
    public static final String PENDING_KILL = "pendingKill";

    /** 记录待响应的杀：targetId 可选择出闪或承受伤害 */
    public void setPendingKill(String targetId, String sourceId, String sourceName, String targetName, int amount) {
        attributes.put(PENDING_KILL, Map.of(
                "targetId", targetId, "sourceId", sourceId, "sourceName", sourceName != null ? sourceName : sourceId,
                "targetName", targetName != null ? targetName : targetId, "amount", amount));
    }

    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getPendingKill() {
        return getAttribute(PENDING_KILL);
    }

    public void clearPendingKill() {
        attributes.remove(PENDING_KILL);
    }

    /** 濒死轮询键名。HP=0 时设置，轮询各玩家是否出桃救人。{ targetId, targetName, askingSeatIndex } */
    public static final String PENDING_DEATH = "pendingDeath";

    public void setPendingDeath(String targetId, String targetName, int askingSeatIndex) {
        attributes.put(PENDING_DEATH, Map.of("targetId", targetId, "targetName", targetName, "askingSeatIndex", askingSeatIndex));
    }

    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getPendingDeath() {
        return getAttribute(PENDING_DEATH);
    }

    public void clearPendingDeath() {
        attributes.remove(PENDING_DEATH);
    }

    /** 记录打出的牌，供中央出牌区展示 */
    public void addPlayedCard(String playerId, String cardId, String cardType, String targetId, String targetName) {
        Map<String, Object> entry = new java.util.HashMap<>();
        entry.put("playerId", playerId);
        entry.put("cardId", cardId);
        entry.put("cardType", cardType);
        if (targetId != null) entry.put("targetId", targetId);
        if (targetName != null) entry.put("targetName", targetName);
        synchronized (recentBattleCards) {
            recentBattleCards.add(entry);
            while (recentBattleCards.size() > BATTLE_CARDS_MAX) {
                recentBattleCards.remove(0);
            }
        }
    }

    public List<Map<String, Object>> getRecentBattleCards() {
        synchronized (recentBattleCards) {
            return new ArrayList<>(recentBattleCards);
        }
    }
}
