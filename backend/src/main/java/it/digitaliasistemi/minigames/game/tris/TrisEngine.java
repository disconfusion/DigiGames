package it.digitaliasistemi.minigames.game.tris;

import com.fasterxml.jackson.databind.JsonNode;
import it.digitaliasistemi.minigames.game.GameContext;
import it.digitaliasistemi.minigames.game.GameEngine;
import it.digitaliasistemi.minigames.leaderboard.LeaderboardService;
import it.digitaliasistemi.minigames.rooms.Room;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tris (filetto): 2 giocatori, griglia 3x3, primo a fare 3 in fila vince. */
@ApplicationScoped
public class TrisEngine implements GameEngine {

    @Inject
    LeaderboardService leaderboard;

    @Override
    public String slug() {
        return "tris";
    }

    @Override
    public int maxPlayers() {
        return 2;
    }

    @Override
    public void onJoin(GameContext ctx) {
        if (ctx.room().game instanceof TrisState ts) {
            ctx.replyToSender(snapshot(ts));
        }
    }

    @Override
    public void onMessage(GameContext ctx, String type, JsonNode payload) {
        Room room = ctx.room();
        switch (type) {
            case "game:start" -> {
                if (room.players.size() < 2) {
                    ctx.replyToSender(error("Servono almeno 2 giocatori per iniziare"));
                    return;
                }
                List<String> seated = new ArrayList<>(room.players);
                boolean vanish = room.options != null && room.options.path("vanish").asBoolean(false);
                TrisState ts = new TrisState(seated.get(0), seated.get(1), vanish);
                room.game = ts;
                room.status = Room.Status.PLAYING;
                ctx.broadcast(snapshot(ts));
            }
            case "move" -> {
                if (!(room.game instanceof TrisState ts)) {
                    ctx.replyToSender(error("Partita non avviata"));
                    return;
                }
                int pos = payload.path("pos").asInt(-1);
                if (pos < 0 || pos > 8) {
                    ctx.replyToSender(error("Casella non valida: deve essere 0..8"));
                    return;
                }
                boolean accepted = ts.place(ctx.senderEmail(), pos);
                if (!accepted) {
                    ctx.replyToSender(error("Mossa non valida: casella occupata o non è il tuo turno"));
                    return;
                }
                ctx.broadcast(snapshot(ts));
                if (ts.status() != TrisState.Status.PLAYING) {
                    room.status = Room.Status.DONE;
                    ctx.broadcast(over(ts));
                    List<String> seated = new ArrayList<>(ts.seats().keySet());
                    if (ts.winner() != null) {
                        for (String p : seated) {
                            leaderboard.record(p, "tris", p.equals(ts.winner()) ? "WIN" : "LOSE");
                        }
                    } else {
                        for (String p : seated) leaderboard.record(p, "tris", "DRAW");
                    }
                }
            }
            default -> ctx.replyToSender(error("Azione sconosciuta: " + type));
        }
    }

    private Map<String, Object> snapshot(TrisState ts) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:state");
        m.put("game", "tris");
        m.put("board", ts.board());
        m.put("currentTurn", ts.currentTurn());
        m.put("seats", ts.seats());
        m.put("status", ts.status().name());
        m.put("winner", ts.winner());
        m.put("vanish", ts.vanish());
        m.put("vanishNext", ts.vanishNext());
        return m;
    }

    private Map<String, Object> over(TrisState ts) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:over");
        m.put("game", "tris");
        m.put("status", ts.status().name());
        m.put("winner", ts.winner());
        return m;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "error");
        m.put("message", msg);
        return m;
    }
}
