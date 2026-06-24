package it.digitaliasistemi.minigames.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.*;
import io.smallrye.jwt.auth.principal.JWTParser;
import it.digitaliasistemi.minigames.social.PresenceService;
import jakarta.inject.Inject;

@WebSocket(path = "/ws/notify")
public class NotifySocket {

    static final UserData.TypedKey<String> USER = UserData.TypedKey.forString("notify.user");

    @Inject JWTParser jwtParser;
    @Inject ObjectMapper mapper;
    @Inject NotifyBus bus;
    @Inject PresenceService presence;

    @OnTextMessage
    public void onMessage(String raw, WebSocketConnection conn) throws Exception {
        JsonNode msg = mapper.readTree(raw);
        if ("hello".equals(msg.path("type").asText())) {
            String token = msg.path("token").asText(null);
            try {
                String username = jwtParser.parse(token).getName();
                conn.userData().put(USER, username);
                bus.register(username, conn);
                presence.touch(username);
                bus.broadcast("presence:update");
                conn.sendTextAndAwait("{\"type\":\"connected\"}");
            } catch (Exception e) {
                conn.sendTextAndAwait("{\"type\":\"error\",\"message\":\"Token non valido\"}");
                conn.closeAndAwait();
            }
        }
    }

    @OnClose
    public void onClose(WebSocketConnection conn) {
        String username = conn.userData().get(USER);
        if (username != null) {
            bus.unregister(username, conn);
            presence.remove(username);
            bus.broadcast("presence:update");
        }
    }
}
