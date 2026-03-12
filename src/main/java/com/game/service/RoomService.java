package com.game.service;

import com.game.domain.GameRoom;
import com.game.domain.Card;
import com.game.engine.GameContext;
import com.game.engine.GameStateMachine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间与对局管理服务。
 * 负责房间的创建、加入、以及每个房间对应的 GameContext / GameStateMachine。
 */
@Service
public class RoomService {

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    /** sessionId -> roomId，用于断开连接时解绑 */
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    /** sessionId -> playerId */
    private final Map<String, String> sessionToPlayer = new ConcurrentHashMap<>();
    /** 房间 ID -> 游戏上下文（开局后创建） */
    private final Map<String, GameContext> contextByRoom = new ConcurrentHashMap<>();
    /** 房间 ID -> 状态机（开局后创建） */
    private final Map<String, GameStateMachine> stateMachineByRoom = new ConcurrentHashMap<>();

    /**
     * 创建房间。
     *
     * @param roomName  房间名
     * @param maxPlayers 最大人数
     * @param ownerId   房主 ID
     * @return 创建好的房间
     */
    public GameRoom createRoom(String roomName, int maxPlayers, String ownerId) {
        GameRoom room = new GameRoom(roomName, maxPlayers, ownerId);
        rooms.put(room.getRoomId(), room);
        return room;
    }

    /**
     * 根据房间 ID 获取房间。
     */
    public Optional<GameRoom> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    /**
     * 加入房间。若房间已满或已开局则返回 empty。
     */
    public Optional<GameRoom> joinRoom(String roomId, String playerId, String nickname) {
        GameRoom room = rooms.get(roomId);
        if (room == null || room.getStatus() != GameRoom.RoomStatus.WAITING) {
            return Optional.empty();
        }
        int seat = room.join(playerId, nickname);
        if (seat < 0) return Optional.empty();
        return Optional.of(room);
    }

    /**
     * 绑定玩家会话到房间（WebSocket 连接后调用），并记录 sessionId 与房间/玩家的对应关系。
     */
    public void bindPlayerSession(String roomId, String playerId, String sessionId) {
        getRoom(roomId).ifPresent(room -> {
            room.bindSession(playerId, sessionId);
            sessionToRoom.put(sessionId, roomId);
            sessionToPlayer.put(sessionId, playerId);
        });
    }

    /**
     * 解绑玩家会话。
     */
    public void unbindPlayerSession(String roomId, String playerId) {
        getRoom(roomId).ifPresent(room -> room.unbindSession(playerId));
    }

    /**
     * 根据 sessionId 解绑并移除记录（断线时调用）。
     */
    public void removeSession(String sessionId) {
        String roomId = sessionToRoom.remove(sessionId);
        String playerId = sessionToPlayer.remove(sessionId);
        if (roomId != null && playerId != null) {
            getRoom(roomId).ifPresent(room -> room.unbindSession(playerId));
        }
    }

    /** 所有房间 ID（用于广播等） */
    public java.util.Set<String> getRoomIds() {
        return java.util.Collections.unmodifiableSet(rooms.keySet());
    }

    /**
     * 获取或创建该房间的游戏上下文（开局时创建）。
     */
    public Optional<GameContext> getOrCreateContext(String roomId) {
        return Optional.ofNullable(contextByRoom.get(roomId));
    }

    /**
     * 开局：仅房主可触发。创建上下文与状态机，初始化牌堆并发 4 张，设为 DRAW 后执行摸 2 张并进入 PLAY。
     *
     * @param requestingPlayerId 请求开局的玩家 ID，必须为房主
     * @return 成功时返回开局结果（含上下文与当前玩家本轮摸到的 2 张牌，用于推送）
     */
    public Optional<StartResult> startGame(String roomId, String requestingPlayerId, GameStarter starter) {
        GameRoom room = rooms.get(roomId);
        if (room == null || room.getStatus() != GameRoom.RoomStatus.WAITING) {
            return Optional.empty();
        }
        if (requestingPlayerId == null || !requestingPlayerId.equals(room.getOwnerId())) {
            return Optional.empty();
        }
        if (room.getPlayerCount() < 1) {
            return Optional.empty();
        }
        room.setStatus(GameRoom.RoomStatus.IN_GAME);
        GameContext ctx = new GameContext(room);
        GameStateMachine sm = new GameStateMachine(ctx);
        if (starter != null) {
            starter.initialize(ctx, sm);
        }
        contextByRoom.put(roomId, ctx);
        stateMachineByRoom.put(roomId, sm);
        List<Card> drawn = sm.processDrawPhase(2);
        return Optional.of(new StartResult(ctx, drawn));
    }

    /** 开局结果：上下文 + 当前玩家本轮摸到的牌（用于向该玩家推送 DRAWN_CARDS） */
    public record StartResult(GameContext context, List<Card> drawnCardsForCurrentPlayer) {}

    /**
     * 获取房间对应的状态机（开局后才有）。
     */
    public Optional<GameStateMachine> getStateMachine(String roomId) {
        return Optional.ofNullable(stateMachineByRoom.get(roomId));
    }

    /**
     * 开局时初始化牌堆、发牌等逻辑的扩展点。
     */
    @FunctionalInterface
    public interface GameStarter {
        void initialize(GameContext context, GameStateMachine stateMachine);
    }
}
