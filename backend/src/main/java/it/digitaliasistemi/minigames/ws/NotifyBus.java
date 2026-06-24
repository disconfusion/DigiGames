package it.digitaliasistemi.minigames.ws;

import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class NotifyBus {
    private final ConcurrentHashMap<String, WebSocketConnection> sessions = new ConcurrentHashMap<>();

    public void register(String username, WebSocketConnection conn) {
        WebSocketConnection old = sessions.put(username, conn);
        if (old != null && !old.equals(conn)) {
            try { old.closeAndAwait(); } catch (Exception ignored) {}
        }
    }

    public void unregister(String username, WebSocketConnection conn) {
        sessions.remove(username, conn);
    }

    public void push(String username, String type) {
        WebSocketConnection conn = sessions.get(username);
        if (conn != null) {
            try {
                conn.sendTextAndAwait("{\"type\":\"" + type + "\"}");
            } catch (Exception ignored) {}
        }
    }
}
