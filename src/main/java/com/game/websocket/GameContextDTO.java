package com.game.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.game.domain.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏上下文快照，用于 WebSocket 全量推送，保证前后端状态强同步。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameContextDTO {

    private String roomId;
    private String currentPhase;
    private int currentSeatIndex;
    private int roundNumber;
    private int drawPileSize;
    private int discardPileSize;
    private List<PlayerSnapshot> players;
    /** 待响应的杀 { targetId, sourceId, sourceName, targetName, amount } */
    private java.util.Map<String, Object> pendingKill;
    /** 濒死轮询 { targetId, targetName, askingSeatIndex } */
    private java.util.Map<String, Object> pendingDeath;
    /** 中央出牌区展示的最近打出的牌 [{ playerId, cardId, cardType, targetId?, targetName? }] */
    private List<java.util.Map<String, Object>> battleCards;

    public java.util.Map<String, Object> getPendingKill() { return pendingKill; }
    public void setPendingKill(java.util.Map<String, Object> pendingKill) { this.pendingKill = pendingKill; }

    public java.util.Map<String, Object> getPendingDeath() { return pendingDeath; }
    public void setPendingDeath(java.util.Map<String, Object> pendingDeath) { this.pendingDeath = pendingDeath; }

    public List<java.util.Map<String, Object>> getBattleCards() { return battleCards; }
    public void setBattleCards(List<java.util.Map<String, Object>> battleCards) { this.battleCards = battleCards; }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }

    public int getCurrentSeatIndex() {
        return currentSeatIndex;
    }

    public void setCurrentSeatIndex(int currentSeatIndex) {
        this.currentSeatIndex = currentSeatIndex;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public int getDrawPileSize() {
        return drawPileSize;
    }

    public void setDrawPileSize(int drawPileSize) {
        this.drawPileSize = drawPileSize;
    }

    public int getDiscardPileSize() {
        return discardPileSize;
    }

    public void setDiscardPileSize(int discardPileSize) {
        this.discardPileSize = discardPileSize;
    }

    public List<PlayerSnapshot> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerSnapshot> players) {
        this.players = players;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PlayerSnapshot {
        private String playerId;
        private String nickname;
        private int seatIndex;
        private int hp;
        private int maxHp;
        private int handCount;
        private boolean alive;
        /** 该玩家当前手牌（MVP 阶段直接下发，方便前端展示和出牌） */
        private List<Card> hand;

        public static PlayerSnapshot from(com.game.domain.Player p) {
            PlayerSnapshot s = new PlayerSnapshot();
            s.setPlayerId(p.getPlayerId());
            s.setNickname(p.getNickname());
            s.setSeatIndex(p.getSeatIndex());
            s.setHp(p.getHp());
            s.setMaxHp(p.getMaxHp());
            s.setHandCount(p.getHandCards().size());
            s.setAlive(p.isAlive());
            s.setHand(new ArrayList<>(p.getHandCards()));
            return s;
        }

        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public int getSeatIndex() { return seatIndex; }
        public void setSeatIndex(int seatIndex) { this.seatIndex = seatIndex; }
        public int getHp() { return hp; }
        public void setHp(int hp) { this.hp = hp; }
        public int getMaxHp() { return maxHp; }
        public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
        public int getHandCount() { return handCount; }
        public void setHandCount(int handCount) { this.handCount = handCount; }
        public boolean isAlive() { return alive; }
        public void setAlive(boolean alive) { this.alive = alive; }
        public List<Card> getHand() { return hand; }
        public void setHand(List<Card> hand) { this.hand = hand; }
    }
}
