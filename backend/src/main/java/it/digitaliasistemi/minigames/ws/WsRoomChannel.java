package it.digitaliasistemi.minigames.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.OpenConnections;
import it.digitaliasistemi.minigames.game.RoomChannel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * Implementazione WebSocket di {@link RoomChannel}: risolve le connessioni della stanza dal
 * path param {@code code} di {@code /ws/room/{code}}.
 *
 * <p>&Egrave; {@code @ApplicationScoped} (a differenza dell'endpoint {@code RoomSocket}, il cui
 * ciclo di vita &egrave; legato alla singola connessione): pu&ograve; quindi essere usata anche
 * fuori dall'handler di un messaggio, per esempio dal loop di gioco del Pong.
 */
@ApplicationScoped
public class WsRoomChannel implements RoomChannel {

    @Inject OpenConnections connections;
    @Inject ObjectMapper mapper;

    @Override
    public void broadcast(String roomCode, Map<String, Object> message) {
        broadcastJson(roomCode, json(message));
    }

    @Override
    public void sendTo(String roomCode, String username, Map<String, Object> message) {
        sendToJson(roomCode, username, json(message));
    }

    @Override
    public boolean hasListeners(String roomCode) {
        return connections.stream().anyMatch(c -> roomCode.equals(c.pathParam("code")));
    }

    /** Variante già serializzata (usata da RoomSocket, che costruisce il JSON per gli eventi di stanza). */
    void broadcastJson(String roomCode, String json) {
        connections.stream()
                .filter(c -> roomCode.equals(c.pathParam("code")))
                .forEach(c -> send(c, json));
    }

    void sendToJson(String roomCode, String username, String json) {
        connections.stream()
                .filter(c -> roomCode.equals(c.pathParam("code")))
                .filter(c -> username.equals(c.userData().get(RoomSocket.USER)))
                .forEach(c -> send(c, json));
    }

    /** Invio best-effort: una connessione morta non deve interrompere il giro sulle altre. */
    private void send(io.quarkus.websockets.next.WebSocketConnection conn, String json) {
        try {
            conn.sendTextAndAwait(json);
        } catch (Exception ignored) {
            /* connessione chiusa nel frattempo */
        }
    }

    String json(Map<String, Object> m) {
        try {
            return mapper.writeValueAsString(m);
        } catch (Exception e) {
            return "{\"type\":\"error\",\"message\":\"serialization\"}";
        }
    }
}
