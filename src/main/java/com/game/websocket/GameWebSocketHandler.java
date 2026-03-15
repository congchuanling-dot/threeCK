package com.game.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.domain.Card;
import com.game.engine.GameContext;
import com.game.domain.GameRoom;
import com.game.domain.Player;
import com.game.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 游戏 WebSocket 处理器。
 * 处理连接、加入房间、接收客户端消息；支持按房间广播。
 */
@Component
public class GameWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RoomService roomService;
    private final WebSocketSessionStore sessionStore;

    public GameWebSocketHandler(RoomService roomService, WebSocketSessionStore sessionStore) {
        this.roomService = roomService;
        this.sessionStore = sessionStore;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        sessionStore.register(sessionId, session, sink);

        Mono<Void> output = session.send(sink.asFlux().map(session::textMessage));
        Mono<Void> input = session.receive()
                .map(msg -> msg.getPayloadAsText())
                .flatMap(text -> handleMessage(sessionId, text))
                .then();

        return Mono.zip(output, input).then()
                .doFinally(signalType -> {
                    sessionStore.unregister(sessionId);
                    roomService.removeSession(sessionId);
                    log.debug("WebSocket session closed: {}", sessionId);
                });
    }

    private Mono<Void> handleMessage(String sessionId, String text) {
        return Mono.fromRunnable(() -> {
            try {
                JsonNode root = MAPPER.readTree(text);
                String action = root.has("action") ? root.get("action").asText() : "";
                switch (action) {
                    case "create_room" -> handleCreateRoom(sessionId, root, sessionId);
                    case "join" -> handleJoin(sessionId, root);
                    case "broadcast" -> { /* 服务端主动广播，客户端仅收；如需客户端发广播可在此解析 */ }
                    default -> log.debug("Unknown action: {}", action);
                }
            } catch (Exception e) {
                log.warn("Parse message failed: {}", text, e);
            }
        });
    }

    private void handleCreateRoom(String sessionId, JsonNode root, String ownerId) {
        String roomName = root.has("roomName") ? root.get("roomName").asText() : "未命名";
        int maxPlayers = root.has("maxPlayers") ? root.get("maxPlayers").asInt(4) : 4;
        String nickname = root.has("nickname") ? root.get("nickname").asText() : "房主";
        GameRoom room = roomService.createRoom(roomName, maxPlayers, ownerId);
        room.join(ownerId, nickname);
        int botSeat = room.join("BOT_1", "机器人");  // 自动加入机器人，方便单人打牌
        roomService.bindPlayerSession(room.getRoomId(), ownerId, sessionId);
        GameMessage msg = GameMessage.roomCreated(room.getRoomId(), room.getRoomName(), room.getMaxPlayers(), ownerId, nickname);
        sendToSession(sessionId, msg);
        sendToSession(sessionId, GameMessage.playerJoined(room.getRoomId(), "BOT_1", "机器人", botSeat));
    }

    private void handleJoin(String sessionId, JsonNode root) {
        String roomId = root.has("roomId") ? root.get("roomId").asText() : "";
        String playerId = root.has("playerId") ? root.get("playerId").asText() : sessionId;
        String nickname = root.has("nickname") ? root.get("nickname").asText() : playerId;
        Optional<GameRoom> roomOpt = roomService.joinRoom(roomId, playerId, nickname);
        if (roomOpt.isEmpty()) {
            sendToSession(sessionId, GameMessage.broadcast("ERROR", Map.of("message", "加入失败")));
            return;
        }
        GameRoom room = roomOpt.get();
        roomService.bindPlayerSession(roomId, playerId, sessionId);
        int seat = room.getPlayerById(playerId).map(p -> p.getSeatIndex()).orElse(-1);
        GameMessage msg = GameMessage.playerJoined(roomId, playerId, nickname, seat);
        sendToSession(sessionId, msg);
        // 可选：广播给房间内其他人
        broadcastToRoom(roomId, msg, sessionId);
    }

    /** 向指定会话发送消息 */
    public void sendToSession(String sessionId, GameMessage message) {
        try {
            String json = MAPPER.writeValueAsString(message);
            sessionStore.sendToSession(sessionId, json);
        } catch (Exception e) {
            log.warn("Send to session failed: {}", sessionId, e);
        }
    }

    /** 向房间内所有会话广播（可选排除某 sessionId） */
    public void broadcastToRoom(String roomId, GameMessage message, String excludeSessionId) {
        roomService.getRoom(roomId).ifPresent(room -> {
            List<String> sessionIds = room.getAllSessionIds();
            try {
                String json = MAPPER.writeValueAsString(message);
                for (String sid : sessionIds) {
                    if (excludeSessionId == null || !excludeSessionId.equals(sid)) {
                        sessionStore.sendToSession(sid, json);
                    }
                }
            } catch (Exception e) {
                log.warn("Broadcast to room {} failed", roomId, e);
            }
        });
    }

    public void broadcastToRoom(String roomId, GameMessage message) {
        broadcastToRoom(roomId, message, null);
    }

    /** 构建 GameContextDTO，用于全量状态推送 */
    public GameContextDTO buildContextDTO(GameContext ctx) {
        GameContextDTO dto = new GameContextDTO();
        dto.setRoomId(ctx.getRoom().getRoomId());
        dto.setCurrentPhase(ctx.getCurrentPhase().name());
        dto.setCurrentSeatIndex(ctx.getCurrentSeatIndex());
        dto.setRoundNumber(ctx.getRoundNumber());
        dto.setDrawPileSize(ctx.getDrawPileSize());
        dto.setDiscardPileSize(ctx.getDiscardPileSize());
        List<GameContextDTO.PlayerSnapshot> list = new ArrayList<>();
        for (Player p : ctx.getRoom().getPlayers()) {
            list.add(GameContextDTO.PlayerSnapshot.from(p));
        }
        dto.setPlayers(list);
        return dto;
    }

    /** 向房间内所有玩家广播最新 GameContext，实现状态强同步 */
    public void broadcastGameContext(String roomId, GameContext ctx) {
        GameContextDTO dto = buildContextDTO(ctx);
        broadcastToRoom(roomId, GameMessage.gameContext(dto));
    }

    /** 向指定玩家推送摸到的牌（仅该玩家收到，用于更新手牌） */
    public void sendDrawnCards(String roomId, String playerId, List<Card> cards) {
        if (cards == null || cards.isEmpty()) return;
        roomService.getRoom(roomId).flatMap(room -> room.getSessionId(playerId))
                .ifPresent(sessionId -> sendToSession(sessionId, GameMessage.drawnCards(cards)));
    }

    /** 向指定玩家推送其手牌全量（如开局时下发初始 4 张） */
    public void sendHandToPlayer(String roomId, String playerId, List<Card> hand) {
        if (hand == null) return;
        roomService.getRoom(roomId).flatMap(room -> room.getSessionId(playerId))
                .ifPresent(sessionId -> sendToSession(sessionId, GameMessage.yourHand(hand)));
    }
}
