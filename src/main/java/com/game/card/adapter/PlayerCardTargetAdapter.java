package com.game.card.adapter;

import com.game.card.Card;
import com.game.card.CardFactory;
import com.game.card.CardTarget;
import com.game.domain.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 将 domain.Player 适配为 CardTarget，使卡牌系统可操作现有玩家。
 * 装备区数据存储在适配器内（因 domain.Player 未包含装备槽）。
 */
public class PlayerCardTargetAdapter implements CardTarget {

    private final Player player;

    /** 玩家装备存储：playerId -> slot -> Card，因 domain.Player 无装备槽 */
    private static final Map<String, Map<String, Card>> EQUIPMENT_STORE = new ConcurrentHashMap<>();

    public PlayerCardTargetAdapter(Player player) {
        this.player = player;
    }

    private Map<String, Card> getEquipment() {
        return EQUIPMENT_STORE.computeIfAbsent(player.getPlayerId(), k -> new ConcurrentHashMap<>());
    }

    @Override
    public String getId() {
        return player.getPlayerId();
    }

    @Override
    public String getNickname() {
        return player.getNickname();
    }

    @Override
    public int getHp() {
        return player.getHp();
    }

    @Override
    public void setHp(int hp) {
        player.setHp(hp);
    }

    @Override
    public void takeDamage(int damage) {
        player.takeDamage(damage);
    }

    @Override
    public int getMaxHp() {
        return player.getMaxHp();
    }

    @Override
    public List<? extends Card> getHandCards() {
        return player.getHandCards().stream()
                .flatMap(dc -> CardFactory.createFromDomainCard(dc).stream())
                .collect(Collectors.toList());
    }

    @Override
    public void addHandCard(Card card) {
        if (card != null) {
            player.addHandCard(card.toDomainCard());
        }
    }

    @Override
    public void addHandCards(List<? extends Card> cards) {
        if (cards != null) {
            for (Card c : cards) {
                addHandCard(c);
            }
        }
    }

    @Override
    public boolean removeHandCard(Card card) {
        return card != null && player.removeHandCardById(card.getId());
    }

    @Override
    public boolean isAlive() {
        return player.isAlive();
    }

    @Override
    public void setAlive(boolean alive) {
        player.setAlive(alive);
    }

    @Override
    public int getSeatIndex() {
        return player.getSeatIndex();
    }

    @Override
    public Card getWeapon() {
        return getEquipment().get("weapon");
    }

    @Override
    public void setWeapon(Card weapon) {
        getEquipment().put("weapon", weapon);
    }

    @Override
    public Card getArmor() {
        return getEquipment().get("armor");
    }

    @Override
    public void setArmor(Card armor) {
        getEquipment().put("armor", armor);
    }

    @Override
    public Card getOffensiveHorse() {
        return getEquipment().get("offensiveHorse");
    }

    @Override
    public void setOffensiveHorse(Card horse) {
        getEquipment().put("offensiveHorse", horse);
    }

    @Override
    public Card getDefensiveHorse() {
        return getEquipment().get("defensiveHorse");
    }

    @Override
    public void setDefensiveHorse(Card horse) {
        getEquipment().put("defensiveHorse", horse);
    }

    @Override
    public boolean isChained() {
        return false; // 待 domain.Player 扩展
    }

    @Override
    public void setChained(boolean chained) {
        // 待 domain.Player 扩展
    }

    @Override
    public boolean isDying() {
        return player.getHp() <= 0 && player.isAlive();
    }

    @Override
    public void setDying(boolean dying) {
        // 待 domain.Player 扩展
    }

    public Player getPlayer() {
        return player;
    }

    /** 判断玩家是否装备诸葛连弩（出杀次数无限） */
    public static boolean hasZhuGeLianNu(String playerId) {
        var eq = EQUIPMENT_STORE.get(playerId);
        if (eq == null) return false;
        Card w = eq.get("weapon");
        return w != null && "诸葛连弩".equals(w.getName());
    }

    /** 获取玩家装备转为 domain.Card 用于序列化，key: weapon/armor/offensiveHorse/defensiveHorse */
    public static java.util.Map<String, com.game.domain.Card> getEquipmentAsDomain(String playerId) {
        var eq = EQUIPMENT_STORE.get(playerId);
        var result = new java.util.HashMap<String, com.game.domain.Card>();
        if (eq == null) return result;
        for (var e : Map.of("weapon", "weapon", "armor", "armor", "offensiveHorse", "offensiveHorse", "defensiveHorse", "defensiveHorse").entrySet()) {
            Card c = eq.get(e.getValue());
            if (c != null) result.put(e.getKey(), c.toDomainCard());
        }
        return result;
    }
}
