package it.digitaliasistemi.minigames.game.chess;

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

/** Scacchi: 2 giocatori, regole complete (arrocco, en passant, promozione, matto/stallo). */
@ApplicationScoped
public class ChessEngine implements GameEngine {

    @Inject
    LeaderboardService leaderboard;

    @Override
    public String slug() {
        return "chess";
    }

    @Override
    public int maxPlayers() {
        return 2;
    }

    @Override
    public void onJoin(GameContext ctx) {
        if (ctx.room().game instanceof ChessState cs) {
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
                List<String> seated = new ArrayList<>(room.players);
                int minutes = room.options != null ? room.options.path("minutesPerPlayer").asInt(0) : 0;
                long millis = Math.max(0, minutes) * 60_000L;
                ChessState cs = new ChessState(seated.get(0), seated.get(1), millis);
                room.game = cs;
                room.status = Room.Status.PLAYING;
                ctx.broadcast(snapshot(cs));
            }
            case "move" -> {
                if (!(room.game instanceof ChessState cs)) {
                    ctx.replyToSender(error("Partita non avviata"));
                    return;
                }
                JsonNode from = payload.path("from");
                JsonNode to = payload.path("to");
                int fr = from.path("r").asInt(-1);
                int fc = from.path("c").asInt(-1);
                int tr = to.path("r").asInt(-1);
                int tc = to.path("c").asInt(-1);
                String promo = payload.path("promotion").asText("");
                char promotion = promo.isEmpty() ? 0 : Character.toUpperCase(promo.charAt(0));

                boolean accepted = cs.move(ctx.senderEmail(), fr, fc, tr, tc, promotion);
                if (!accepted) {
                    ctx.replyToSender(error("Mossa non valida"));
                    return;
                }
                broadcastAndMaybeEnd(ctx, room, cs);
            }
            case "resign" -> {
                if (room.game instanceof ChessState cs && cs.resign(ctx.senderEmail())) {
                    broadcastAndMaybeEnd(ctx, room, cs);
                }
            }
            case "draw:offer" -> {
                if (room.game instanceof ChessState cs && cs.offerDraw(ctx.senderEmail())) {
                    ctx.broadcast(snapshot(cs));
                }
            }
            case "draw:accept" -> {
                if (room.game instanceof ChessState cs && cs.respondDraw(ctx.senderEmail(), true)) {
                    broadcastAndMaybeEnd(ctx, room, cs);
                }
            }
            case "draw:decline" -> {
                if (room.game instanceof ChessState cs && cs.respondDraw(ctx.senderEmail(), false)) {
                    ctx.broadcast(snapshot(cs));
                }
            }
            case "clock:claim" -> {
                if (room.game instanceof ChessState cs && cs.checkTimeout()) {
                    broadcastAndMaybeEnd(ctx, room, cs);
                }
            }
            default -> ctx.replyToSender(error("Azione sconosciuta: " + type));
        }
    }

    private void broadcastAndMaybeEnd(GameContext ctx, Room room, ChessState cs) {
        ctx.broadcast(snapshot(cs));
        if (cs.status() != ChessState.Status.PLAYING) {
            room.status = Room.Status.DONE;
            ctx.broadcast(over(cs));
            recordResults(cs);
        }
    }

    private void recordResults(ChessState cs) {
        Character winnerColor = cs.winner();
        for (var e : cs.seats().entrySet()) {
            String player = e.getKey();
            String result;
            if (winnerColor == null) {
                result = "DRAW"; // stallo
            } else {
                result = e.getValue().equals(winnerColor) ? "WIN" : "LOSE";
            }
            leaderboard.record(player, "chess", result);
        }
    }

    private Map<String, Object> snapshot(ChessState cs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:state");
        m.put("game", "chess");
        m.put("board", cs.board());
        m.put("currentTurn", cs.currentTurn());
        Map<String, Object> seatStr = new LinkedHashMap<>();
        cs.seats().forEach((k, v) -> seatStr.put(k, String.valueOf(v)));
        m.put("seats", seatStr);
        m.put("status", cs.status().name());
        m.put("winner", cs.winner() == null ? null : String.valueOf(cs.winner()));
        m.put("inCheck", cs.inCheck());
        m.put("legalMoves", cs.legalMovesView());
        m.put("drawOfferBy", cs.drawOfferBy() == null ? null : String.valueOf(cs.drawOfferBy()));
        m.put("clock", cs.clockView());
        return m;
    }

    private Map<String, Object> over(ChessState cs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:over");
        m.put("game", "chess");
        m.put("status", cs.status().name());
        m.put("winner", cs.winner() == null ? null : String.valueOf(cs.winner()));
        return m;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "error");
        m.put("message", msg);
        return m;
    }
}
