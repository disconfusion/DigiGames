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
import jakarta.enterprise.context.control.ActivateRequestContext;
import it.digitaliasistemi.minigames.game.GameContext;
import it.digitaliasistemi.minigames.game.GameEngine;
import it.digitaliasistemi.minigames.game.GameEngines;
import it.digitaliasistemi.minigames.rooms.Room;
import it.digitaliasistemi.minigames.rooms.RoomManager;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebSocket(path = "/ws/room/{code}")
public class RoomSocket {

    static final UserData.TypedKey<String> USER = UserData.TypedKey.forString("user");
    // Segna che il giocatore ha lasciato la stanza esplicitamente (msg "leave"/"room:close"):
    // così onClose non ri-elabora la rimozione alla chiusura del socket.
    static final UserData.TypedKey<Boolean> LEFT = UserData.TypedKey.forBoolean("left");

    @Inject RoomManager rooms;
    @Inject OpenConnections connections;
    @Inject ObjectMapper mapper;
    @Inject JWTParser jwtParser;
    @Inject GameEngines engines;

    // Gli handler WebSocket girano su executor-thread senza il request context CDI dei REST:
    // senza questo, ogni accesso Panache dagli engine (es. inventario poteri) lancia
    // ContextNotActiveException. I metodi @Transactional dei service gestiscono la loro tx.
    @ActivateRequestContext
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
                String username;
                try {
                    username = jwtParser.parse(token).getName();
                } catch (Exception e) {
                    // Solo il parsing del token: gli errori dell'engine (onJoin) NON vanno
                    // mascherati da "Token non valido".
                    conn.sendTextAndAwait(err("Token non valido"));
                    conn.closeAndAwait();
                    return;
                }
                conn.userData().put(USER, username);
                room.players.add(username);
                broadcast(code, evt("player:joined", "username", username, "players", room.players.size()));
                // Snapshot completo dei membri al solo nuovo socket: chi entra dopo (o ricarica la
                // pagina) altrimenti non conoscerebbe i giocatori già presenti, perché i loro
                // "player:joined" sono stati trasmessi prima che questo socket esistesse.
                conn.sendTextAndAwait(evt("room:members", "players", List.copyOf(room.players)));
                GameEngine engine = engines.get(room.gameSlug);
                if (engine != null) engine.onJoin(ctx(conn, room, username));
            }
            case "chat" -> {
                String username = requireAuth(conn);
                if (username == null) return;
                broadcast(code, evt("chat", "from", username, "text", msg.path("text").asText("")));
            }
            case "leave" -> {
                // Uscita esplicita: rimuove il membro in QUALSIASI stato (anche PLAYING), a differenza
                // della chiusura implicita del socket che lo tiene sospeso per il rientro.
                String username = requireAuth(conn);
                if (username == null) return;
                conn.userData().put(LEFT, true);
                leaveRoom(code, room, username, conn);
            }
            case "room:close" -> {
                // Solo l'host può chiudere la stanza per tutti.
                String username = requireAuth(conn);
                if (username == null) return;
                if (!username.equals(room.hostEmail)) {
                    conn.sendTextAndAwait(err("Solo l'host può chiudere la stanza"));
                    return;
                }
                // LEFT solo dopo la validazione: un rifiuto non deve bloccare il cleanup del suo slot.
                conn.userData().put(LEFT, true);
                broadcast(code, evt("room:closed", "by", username));
                rooms.remove(code);
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
        if (username == null || room == null) return;
        // Uscita esplicita già gestita da "leave"/"room:close": non ri-elaborare.
        if (Boolean.TRUE.equals(conn.userData().get(LEFT))) return;
        // Partita in corso: il giocatore resta membro (slot "sospeso") e la stanza sopravvive,
        // così può rientrare dalla home ("Partite in corso"). Per gli altri resta seduto: niente broadcast.
        if (room.status == Room.Status.PLAYING) return;
        // Lobby (WAITING) o partita finita (DONE): libera lo slot e distruggi la stanza se vuota.
        leaveRoom(code, room, username, conn);
    }

    /** Rimuove il giocatore dalla stanza, notifica gli altri e distrugge la stanza se resta vuota. */
    private void leaveRoom(String code, Room room, String username, WebSocketConnection conn) {
        boolean wasPlaying = room.status == Room.Status.PLAYING;
        room.players.remove(username);
        broadcast(code, evt("player:left", "username", username, "players", room.players.size()));
        if (room.players.isEmpty()) {
            rooms.remove(code);
            return;
        }
        // Uscita esplicita a partita in corso: l'engine aggiorna il roster dei turni (niente turno
        // fantasma / stallo). Alla chiusura implicita del socket wasPlaying è false (onClose esce
        // prima durante PLAYING), quindi qui non tocchiamo lo stato di gioco.
        if (wasPlaying) {
            GameEngine engine = engines.get(room.gameSlug);
            if (engine != null) engine.onLeave(ctx(conn, room, username));
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
