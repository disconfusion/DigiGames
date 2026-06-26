package it.digitaliasistemi.minigames.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class NotifyBus {
    private final ConcurrentHashMap<String, WebSocketConnection> sessions = new ConcurrentHashMap<>();

    @Inject ObjectMapper mapper;

    public void register(String username, WebSocketConnection conn) {
        WebSocketConnection old = sessions.put(username, conn);
        if (old != null && !old.equals(conn)) {
            try { old.closeAndAwait(); } catch (Exception ignored) {}
        }
    }

    public void unregister(String username, WebSocketConnection conn) {
        sessions.remove(username, conn);
    }

    /** Notifica un singolo utente con il solo tipo. */
    public void push(String username, String type) {
        push(username, type, null);
    }

    /** Notifica un singolo utente con tipo + dati extra (es. mittente invito). */
    public void push(String username, String type, Map<String, ?> data) {
        WebSocketConnection conn = sessions.get(username);
        if (conn != null) {
            try { conn.sendTextAndAwait(json(type, data)); } catch (Exception ignored) {}
        }
    }

    public void broadcast(String type) {
        String payload = json(type, null);
        sessions.values().forEach(conn -> {
            try { conn.sendTextAndAwait(payload); } catch (Exception ignored) {}
        });
    }

    /** Serializza in modo sicuro {type, ...data} (evita injection da displayName con apici). */
    private String json(String type, Map<String, ?> data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        if (data != null) m.putAll(data);
        try {
            return mapper.writeValueAsString(m);
        } catch (Exception e) {
            return "{\"type\":\"" + type + "\"}";
        }
    }
}
