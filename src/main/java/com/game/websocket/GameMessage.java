package com.game.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * WebSocket 消息统一 DTO。
 * type 区分业务类型，payload 为 JSON 体。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameMessage {

    private String type;
    private Object payload;

    public GameMessage() {}

    public GameMessage(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    /** 房间创建成功 */
    public static GameMessage roomCreated(String roomId, String roomName, int maxPlayers, String ownerId, String ownerNickname) {
        return new GameMessage("ROOM_CREATED", new RoomCreatedMessage(roomId, roomName, maxPlayers, ownerId, ownerNickname));
    }

    /** 玩家加入房间 */
    public static GameMessage playerJoined(String roomId, String playerId, String nickname, int seatIndex) {
        return new GameMessage("PLAYER_JOINED", new PlayerJoinedMessage(roomId, playerId, nickname, seatIndex));
    }

    /** 阶段变化（广播） */
    public static GameMessage phaseChanged(String phase, int currentSeatIndex, int roundNumber) {
        return new GameMessage("PHASE_CHANGED", new PhaseChangedMessage(phase, currentSeatIndex, roundNumber));
    }

    /** 通用广播 */
    public static GameMessage broadcast(String messageType, Object data) {
        return new GameMessage("BROADCAST", new BroadcastMessage(messageType, data));
    }

    /** 全量游戏上下文（状态强同步） */
    public static GameMessage gameContext(GameContextDTO dto) {
        return new GameMessage("GAME_CONTEXT", dto);
    }

    /** 摸牌推送（仅发给该玩家，payload 为摸到的牌列表） */
    public static GameMessage drawnCards(java.util.List<?> cards) {
        return new GameMessage("DRAWN_CARDS", cards);
    }

    /** 手牌全量推送（仅发给该玩家，如开局时下发初始手牌） */
    public static GameMessage yourHand(java.util.List<?> cards) {
        return new GameMessage("YOUR_HAND", cards);
    }

    // --- 内部 payload 类型 ---

    public record RoomCreatedMessage(String roomId, String roomName, int maxPlayers, String ownerId, String ownerNickname) {}
    public record PlayerJoinedMessage(String roomId, String playerId, String nickname, int seatIndex) {}
    public record PhaseChangedMessage(String phase, int currentSeatIndex, int roundNumber) {}
    public record BroadcastMessage(String messageType, Object data) {}
}
