package it.digitaliasistemi.minigames.game.minesweeper;

import com.fasterxml.jackson.databind.JsonNode;
import it.digitaliasistemi.minigames.game.GameContext;
import it.digitaliasistemi.minigames.game.GameEngine;
import it.digitaliasistemi.minigames.rooms.Room;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Campo Minato co-op: fino a 8 giocatori rivelano la stessa board.
 * Il server è l'unica autorità; le mine non vengono mai trasmesse finché la partita è in corso.
 */
@ApplicationScoped
public class MinesweeperEngine implements GameEngine {

    @Override
    public String slug() {
        return "minesweeper";
    }

    @Override
    public int maxPlayers() {
        return 8;
    }

    @Override
    public void onJoin(GameContext ctx) {
        if (ctx.room().game instanceof MinesweeperState ms) {
            ctx.replyToSender(buildState(ms));
        }
    }

    @Override
    public void onMessage(GameContext ctx, String type, JsonNode payload) {
        Room room = ctx.room();

        switch (type) {

            case "game:start" -> {
                MinesweeperState ms = new MinesweeperState();
                room.game   = ms;
                room.status = Room.Status.PLAYING;
                ctx.broadcast(buildState(ms));
            }

            case "reveal" -> {
                if (!(room.game instanceof MinesweeperState ms)) {
                    ctx.replyToSender(error("Partita non avviata"));
                    return;
                }
                int r = payload.path("r").asInt(-1);
                int c = payload.path("c").asInt(-1);
                if (!ms.reveal(r, c)) return; // ignorato (già rivelata, flagged, o fuori bounds)
                ctx.broadcast(buildState(ms));
                if (ms.status() != MinesweeperState.Status.PLAYING) {
                    ctx.broadcast(buildOver(ms));
                }
            }

            case "flag" -> {
                if (!(room.game instanceof MinesweeperState ms)) {
                    ctx.replyToSender(error("Partita non avviata"));
                    return;
                }
                int r = payload.path("r").asInt(-1);
                int c = payload.path("c").asInt(-1);
                if (!ms.flag(r, c)) return; // ignorato
                ctx.broadcast(buildState(ms));
            }

            default -> ctx.replyToSender(error("Azione sconosciuta: " + type));
        }
    }

    // ----------------------------------------------------------------
    // Costruzione snapshot
    // ----------------------------------------------------------------

    private Map<String, Object> buildState(MinesweeperState ms) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type",       "game:state");
        m.put("game",       "minesweeper");
        m.put("rows",       MinesweeperState.ROWS);
        m.put("cols",       MinesweeperState.COLS);
        m.put("minesTotal", MinesweeperState.MINES);
        m.put("flagsUsed",  ms.flagsUsed());
        m.put("status",     ms.status().name());
        m.put("cells",      serializeCells(ms.cellSnapshot()));
        return m;
    }

    private Map<String, Object> buildOver(MinesweeperState ms) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type",   "game:over");
        m.put("game",   "minesweeper");
        m.put("status", ms.status().name());
        return m;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type",    "error");
        m.put("message", msg);
        return m;
    }

    /**
     * Serializza la matrice di CellSnapshot in una List<List<Map>> compatibile con Jackson.
     * I campi opzionali (adjacent, mine) vengono omessi quando null per non esporre mine
     * nascoste durante la partita.
     */
    private List<List<Map<String, Object>>> serializeCells(MinesweeperState.CellSnapshot[][] snap) {
        List<List<Map<String, Object>>> rows = new ArrayList<>(snap.length);
        for (MinesweeperState.CellSnapshot[] row : snap) {
            List<Map<String, Object>> cols = new ArrayList<>(row.length);
            for (MinesweeperState.CellSnapshot cell : row) {
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("revealed", cell.revealed());
                cm.put("flagged",  cell.flagged());
                if (cell.adjacent() != null) cm.put("adjacent", cell.adjacent());
                if (cell.mine()     != null) cm.put("mine",     cell.mine());
                cols.add(cm);
            }
            rows.add(cols);
        }
        return rows;
    }
}
