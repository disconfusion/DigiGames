package it.digitaliasistemi.minigames.game.connect4;

import com.fasterxml.jackson.databind.JsonNode;
import it.digitaliasistemi.minigames.game.GameContext;
import it.digitaliasistemi.minigames.game.GameEngine;
import it.digitaliasistemi.minigames.rooms.Room;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Forza 4: 2 giocatori, board 6x7, primo a fare 4 di fila vince. */
@ApplicationScoped
public class Connect4Engine implements GameEngine {

    @Override
    public String slug() {
        return "connect4";
    }

    @Override
    public int maxPlayers() {
        return 2;
    }

    @Override
    public void onJoin(GameContext ctx) {
        if (ctx.room().game instanceof Connect4State cs) {
            ctx.replyToSender(snapshot(cs));
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
                // Siedi i primi 2 giocatori in ordine d'iscrizione
                List<String> seated = new ArrayList<>(room.players);
                String player1 = seated.get(0);
                String player2 = seated.get(1);
                Connect4State cs = new Connect4State(player1, player2);
                room.game = cs;
                room.status = Room.Status.PLAYING;
                ctx.broadcast(snapshot(cs));
            }
            case "move" -> {
                if (!(room.game instanceof Connect4State cs)) {
                    ctx.replyToSender(error("Partita non avviata"));
                    return;
                }
                int col = payload.path("col").asInt(-1);
                if (col < 0 || col >= Connect4State.COLS) {
                    ctx.replyToSender(error("Colonna non valida: deve essere 0..6"));
                    return;
                }
                boolean accepted = cs.drop(ctx.senderEmail(), col);
                if (!accepted) {
                    ctx.replyToSender(error("Mossa non valida: colonna piena o non è il tuo turno"));
                    return;
                }
                ctx.broadcast(snapshot(cs));
                if (cs.status() != Connect4State.Status.PLAYING) {
                    room.status = Room.Status.DONE;
                    ctx.broadcast(over(cs));
                }
            }
            default -> ctx.replyToSender(error("Azione sconosciuta: " + type));
        }
    }

    private Map<String, Object> snapshot(Connect4State cs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:state");
        m.put("game", "connect4");
        m.put("board", cs.board());
        m.put("currentTurn", cs.currentTurn());
        m.put("seats", cs.seats());
        m.put("status", cs.status().name());
        m.put("winner", cs.winner());
        return m;
    }

    private Map<String, Object> over(Connect4State cs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:over");
        m.put("game", "connect4");
        m.put("status", cs.status().name());
        m.put("winner", cs.winner());
        return m;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "error");
        m.put("message", msg);
        return m;
    }
}
