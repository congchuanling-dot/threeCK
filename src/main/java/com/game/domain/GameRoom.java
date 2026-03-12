package com.game.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 游戏房间。
 * 管理房间内玩家、房间状态，与 GameContext 一一对应。
 */
public class GameRoom {

    private final String roomId;
    private final String roomName;
    private final int maxPlayers;
    /** 座位上的玩家，按座位序排列，空位为 null */
    private final List<Player> seats = new CopyOnWriteArrayList<>();
    /** 玩家 ID -> 会话 ID（用于 WebSocket 推送） */
    private final ConcurrentHashMap<String, String> playerSessionIds = new ConcurrentHashMap<>();
    /** 房间状态 */
    private volatile RoomStatus status = RoomStatus.WAITING;
    /** 房主玩家 ID */
    private volatile String ownerId;

    private static final AtomicInteger ROOM_ID_GEN = new AtomicInteger(10000);

    public enum RoomStatus {
        /** 等待玩家加入 */
        WAITING,
        /** 游戏中 */
        IN_GAME,
        /** 已结束 */
        FINISHED
    }

    public GameRoom(String roomName, int maxPlayers, String ownerId) {
        this.roomId = "R" + ROOM_ID_GEN.incrementAndGet();
        this.roomName = roomName != null ? roomName : this.roomId;
        this.maxPlayers = Math.max(2, Math.min(maxPlayers, 8));
        this.ownerId = ownerId;
        for (int i = 0; i < this.maxPlayers; i++) {
            seats.add(null);
        }
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public String getOwnerId() {
        return ownerId;
    }

    /** 当前房间内玩家列表（不含空位） */
    public List<Player> getPlayers() {
        List<Player> list = new ArrayList<>();
        for (Player p : seats) {
            if (p != null) list.add(p);
        }
        return Collections.unmodifiableList(list);
    }

    /** 按座位索引取玩家 */
    public Optional<Player> getPlayerBySeat(int seatIndex) {
        if (seatIndex < 0 || seatIndex >= seats.size()) return Optional.empty();
        return Optional.ofNullable(seats.get(seatIndex));
    }

    /** 按玩家 ID 查找 */
    public Optional<Player> getPlayerById(String playerId) {
        return seats.stream()
                .filter(p -> p != null && p.getPlayerId().equals(playerId))
                .findFirst();
    }

    /** 加入房间，返回分配的座位号；已满返回 -1 */
    public int join(String playerId, String nickname) {
        if (status != RoomStatus.WAITING) return -1;
        if (getPlayerById(playerId).isPresent()) return getPlayerById(playerId).get().getSeatIndex();
        for (int i = 0; i < seats.size(); i++) {
            if (seats.get(i) == null) {
                seats.set(i, new Player(playerId, nickname, i, 4));
                if (ownerId == null) ownerId = playerId;
                return i;
            }
        }
        return -1;
    }

    /** 绑定玩家会话 ID（WebSocket session） */
    public void bindSession(String playerId, String sessionId) {
        playerSessionIds.put(playerId, sessionId);
    }

    /** 解绑会话 */
    public void unbindSession(String playerId) {
        playerSessionIds.remove(playerId);
    }

    /** 获取玩家当前会话 ID */
    public Optional<String> getSessionId(String playerId) {
        return Optional.ofNullable(playerSessionIds.get(playerId));
    }

    /** 所有已绑定的会话 ID（用于广播） */
    public List<String> getAllSessionIds() {
        return new ArrayList<>(playerSessionIds.values());
    }

    /** 当前人数 */
    public int getPlayerCount() {
        return (int) seats.stream().filter(p -> p != null).count();
    }
}
