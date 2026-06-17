package it.digitaliasistemi.minigames.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.UserData;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.jwt.auth.principal.JWTParser;
import it.digitaliasistemi.minigames.game.GameContext;
import it.digitaliasistemi.minigames.game.GameEngine;
import it.digitaliasistemi.minigames.game.GameEngines;
import it.digitaliasistemi.minigames.rooms.Room;
import it.digitaliasistemi.minigames.rooms.RoomManager;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;

@WebSocket(path = "/ws/room/{code}")
public class RoomSocket {

    static final UserData.TypedKey<String> USER = UserData.TypedKey.forString("user");

    @Inject RoomManager rooms;
    @Inject OpenConnections connections;
    @Inject ObjectMapper mapper;
    @Inject JWTParser jwtParser;
    @Inject GameEngines engines;

    @OnTextMessage
    public void onMessage(String raw, WebSocketConnection conn) throws Exception {
        JsonNode msg = mapper.readTree(raw);
        String type = msg.path("type").asText("");
        String code = conn.pathParam("code");
        Room room = rooms.get(code);
        if (room == null) {
            conn.sendTextAndAwait(err("Stanza inesistente"));
            return;
        }

        switch (type) {
            case "hello" -> {
                String token = msg.path("token").asText(null);
                try {
                    String username = jwtParser.parse(token).getName();
                    conn.userData().put(USER, username);
                    room.players.add(username);
                    broadcast(code, evt("player:joined", "username", username, "players", room.players.size()));
                    GameEngine engine = engines.get(room.gameSlug);
                    if (engine != null) engine.onJoin(ctx(conn, room, username));
                } catch (Exception e) {
                    conn.sendTextAndAwait(err("Token non valido"));
                    conn.closeAndAwait();
                }
            }
            case "chat" -> {
                String username = requireAuth(conn);
                if (username == null) return;
                broadcast(code, evt("chat", "from", username, "text", msg.path("text").asText("")));
            }
            default -> {
                String username = requireAuth(conn);
                if (username == null) return;
                GameEngine engine = engines.get(room.gameSlug);
                if (engine == null) {
                    conn.sendTextAndAwait(err("Gioco non supportato: " + room.gameSlug));
                    return;
                }
                engine.onMessage(ctx(conn, room, username), type, msg);
            }
        }
    }

    @OnClose
    public void onClose(WebSocketConnection conn) {
        String username = conn.userData().get(USER);
        String code = conn.pathParam("code");
        Room room = rooms.get(code);
        if (username != null && room != null) {
            room.players.remove(username);
            broadcast(code, evt("player:left", "username", username, "players", room.players.size()));
            if (room.players.isEmpty()) rooms.remove(code);
        }
    }

    private GameContext ctx(WebSocketConnection conn, Room room, String username) {
        return new GameContext() {
            @Override public Room room() { return room; }
            @Override public String senderEmail() { return username; }
            @Override public void replyToSender(Map<String, Object> message) {
                conn.sendTextAndAwait(toJson(message));
            }
            @Override public void broadcast(Map<String, Object> message) {
                RoomSocket.this.broadcast(room.code, toJson(message));
            }
            @Override public void sendTo(String target, Map<String, Object> message) {
                String json = toJson(message);
                connections.stream()
                        .filter(c -> room.code.equals(c.pathParam("code")))
                        .filter(c -> target.equals(c.userData().get(USER)))
                        .forEach(c -> c.sendTextAndAwait(json));
            }
        };
    }

    private String requireAuth(WebSocketConnection conn) {
        String username = conn.userData().get(USER);
        if (username == null) {
            conn.sendTextAndAwait(err("Non autenticato: invia prima un messaggio 'hello' col token"));
        }
        return username;
    }

    private void broadcast(String code, String json) {
        connections.stream()
                .filter(c -> code.equals(c.pathParam("code")))
                .forEach(c -> c.sendTextAndAwait(json));
    }

    private String evt(String type, Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return toJson(m);
    }

    private String err(String message) { return evt("error", "message", message); }

    private String toJson(Map<String, Object> m) {
        try {
            return mapper.writeValueAsString(m);
        } catch (Exception e) {
            return "{\"type\":\"error\",\"message\":\"serialization\"}";
        }
    }
}
