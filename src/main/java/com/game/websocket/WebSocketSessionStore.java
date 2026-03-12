package com.game.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话存储。
 * 维护 sessionId -> session 的映射，用于向指定会话推送消息。
 */
@Component
public class WebSocketSessionStore {

    /** sessionId -> WebSocketSession */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /** sessionId -> Sink（用于向该会话发送消息） */
    private final Map<String, Sinks.Many<String>> sinks = new ConcurrentHashMap<>();

    public void register(String sessionId, WebSocketSession session, Sinks.Many<String> sink) {
        sessions.put(sessionId, session);
        sinks.put(sessionId, sink);
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);
        Sinks.Many<String> s = sinks.remove(sessionId);
        if (s != null) {
            s.tryEmitComplete();
        }
    }

    public WebSocketSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public Sinks.Many<String> getSink(String sessionId) {
        return sinks.get(sessionId);
    }

    /** 向指定 session 发送文本消息 */
    public void sendToSession(String sessionId, String text) {
        Sinks.Many<String> sink = sinks.get(sessionId);
        if (sink != null) {
            sink.tryEmitNext(text);
        }
    }
}
