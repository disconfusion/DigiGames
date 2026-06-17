package it.digitaliasistemi.minigames.game.dama;

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

/** Dama italiana: 2 giocatori, 8x8, presa obbligatoria. */
@ApplicationScoped
public class DamaEngine implements GameEngine {

    @Inject
    LeaderboardService leaderboard;

    @Override
    public String slug() {
        return "dama";
    }

    @Override
    public int maxPlayers() {
        return 2;
    }

    @Override
    public void onJoin(GameContext ctx) {
        if (ctx.room().game instanceof DamaState ds) {
            ctx.replyToSender(snapshot(ds));
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
                DamaState ds = new DamaState(seated.get(0), seated.get(1));
                room.game = ds;
                room.status = Room.Status.PLAYING;
                ctx.broadcast(snapshot(ds));
            }
            case "move" -> {
                if (!(room.game instanceof DamaState ds)) {
                    ctx.replyToSender(error("Partita non avviata"));
                    return;
                }
                JsonNode from = payload.path("from");
                JsonNode to = payload.path("to");
                int fr = from.path("r").asInt(-1);
                int fc = from.path("c").asInt(-1);
                int tr = to.path("r").asInt(-1);
                int tc = to.path("c").asInt(-1);
                boolean accepted = ds.move(ctx.senderEmail(), fr, fc, tr, tc);
                if (!accepted) {
                    ctx.replyToSender(error("Mossa non valida (turno, geometria o presa obbligatoria)"));
                    return;
                }
                ctx.broadcast(snapshot(ds));
                if (ds.status() != DamaState.Status.PLAYING) {
                    room.status = Room.Status.DONE;
                    ctx.broadcast(over(ds));
                    List<String> seated = new ArrayList<>(ds.seats().keySet());
                    for (String p : seated) {
                        leaderboard.record(p, "dama", p.equals(ds.winner()) ? "WIN" : "LOSE");
                    }
                }
            }
            default -> ctx.replyToSender(error("Azione sconosciuta: " + type));
        }
    }

    private Map<String, Object> snapshot(DamaState ds) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:state");
        m.put("game", "dama");
        m.put("board", ds.board());
        m.put("currentTurn", ds.currentTurn());
        m.put("seats", ds.seats());
        m.put("status", ds.status().name());
        m.put("winner", ds.winner());
        m.put("mustContinue", ds.mustContinue());
        return m;
    }

    private Map<String, Object> over(DamaState ds) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:over");
        m.put("game", "dama");
        m.put("status", ds.status().name());
        m.put("winner", ds.winner());
        return m;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "error");
        m.put("message", msg);
        return m;
    }
}
