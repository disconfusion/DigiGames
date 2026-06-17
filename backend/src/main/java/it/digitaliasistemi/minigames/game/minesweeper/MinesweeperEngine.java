package it.digitaliasistemi.minigames.game.minesweeper;

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

@ApplicationScoped
public class MinesweeperEngine implements GameEngine {

    @Inject
    LeaderboardService leaderboard;

    @Override public String slug() { return "minesweeper"; }
    @Override public int maxPlayers() { return 8; }

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
                MinesweeperState ms = new MinesweeperState(new ArrayList<>(room.players));
                room.game   = ms;
                room.status = Room.Status.PLAYING;
                ctx.broadcast(buildState(ms));
            }

            case "reveal" -> {
                if (!(room.game instanceof MinesweeperState ms)) {
                    ctx.replyToSender(error("Partita non avviata"));
                    return;
                }
                if (!ms.isMyTurn(ctx.senderEmail())) {
                    ctx.replyToSender(error("Non è il tuo turno"));
                    return;
                }
                int r = payload.path("r").asInt(-1);
                int c = payload.path("c").asInt(-1);
                if (!ms.reveal(r, c)) return;
                ctx.broadcast(buildState(ms));
                if (ms.status() != MinesweeperState.Status.PLAYING) {
                    ctx.broadcast(buildOver(ms));
                    room.status = Room.Status.DONE;
                    String lbResult = ms.status() == MinesweeperState.Status.WON ? "WIN" : "LOSE";
                    for (String p : room.players) leaderboard.record(p, "minesweeper", lbResult);
                }
            }

            case "flag" -> {
                if (!(room.game instanceof MinesweeperState ms)) {
                    ctx.replyToSender(error("Partita non avviata"));
                    return;
                }
                if (!ms.isMyTurn(ctx.senderEmail())) {
                    ctx.replyToSender(error("Non è il tuo turno"));
                    return;
                }
                int r = payload.path("r").asInt(-1);
                int c = payload.path("c").asInt(-1);
                if (!ms.flag(r, c)) return;
                ctx.broadcast(buildState(ms));
            }

            default -> ctx.replyToSender(error("Azione sconosciuta: " + type));
        }
    }

    private Map<String, Object> buildState(MinesweeperState ms) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type",        "game:state");
        m.put("game",        "minesweeper");
        m.put("rows",        MinesweeperState.ROWS);
        m.put("cols",        MinesweeperState.COLS);
        m.put("minesTotal",  MinesweeperState.MINES);
        m.put("flagsUsed",   ms.flagsUsed());
        m.put("status",      ms.status().name());
        m.put("currentTurn", ms.currentTurn());
        m.put("cells",       serializeCells(ms.cellSnapshot()));
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
