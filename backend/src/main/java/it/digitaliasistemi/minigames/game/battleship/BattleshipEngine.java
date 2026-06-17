package it.digitaliasistemi.minigames.game.battleship;

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

/**
 * Battaglia Navale: 2 giocatori, board 10x10, flotta [5,4,3,3,2], informazione nascosta.
 *
 * <p>Protocollo a SNAPSHOT completi MA per-giocatore: lo stato NON va mai in broadcast
 * (rivelerebbe le navi). Ad ogni cambiamento si invia con {@link GameContext#sendTo} a
 * CIASCUNO dei due giocatori la SUA vista personalizzata (le navi avversarie non colpite
 * non sono mai incluse).
 */
@ApplicationScoped
public class BattleshipEngine implements GameEngine {

    @Inject
    LeaderboardService leaderboard;

    @Override
    public String slug() {
        return "battleship";
    }

    @Override
    public int maxPlayers() {
        return 2;
    }

    @Override
    public void onJoin(GameContext ctx) {
        // Allinea il nuovo arrivato allo stato corrente inviandogli la SUA vista.
        if (ctx.room().game instanceof BattleshipState bs) {
            ctx.replyToSender(viewFor(bs, ctx.senderEmail()));
        }
    }

    @Override
    public void onMessage(GameContext ctx, String type, JsonNode payload) {
        Room room = ctx.room();
        switch (type) {
            case "game:start" -> {
                if (room.players.size() < 2) {
                    ctx.replyToSender(error("Servono 2 giocatori per iniziare"));
                    return;
                }
                List<String> seated = new ArrayList<>(room.players);
                BattleshipState bs = new BattleshipState(seated.get(0), seated.get(1));
                room.game = bs;
                room.status = Room.Status.PLAYING;
                sendStateToBoth(ctx, bs);
            }
            case "randomize" -> {
                BattleshipState bs = require(ctx);
                if (bs == null) return;
                if (!bs.randomize(ctx.senderEmail())) {
                    ctx.replyToSender(error("Impossibile disporre la flotta ora"));
                    return;
                }
                sendStateToBoth(ctx, bs);
            }
            case "place" -> {
                BattleshipState bs = require(ctx);
                if (bs == null) return;
                List<int[]> ships = parseShips(payload);
                if (ships == null) {
                    ctx.replyToSender(error("Formato navi non valido"));
                    return;
                }
                if (!bs.placeFleet(ctx.senderEmail(), ships)) {
                    ctx.replyToSender(error("Disposizione non valida: navi fuori bordo, sovrapposte o flotta errata (richiesta [5,4,3,3,2])"));
                    return;
                }
                sendStateToBoth(ctx, bs);
            }
            case "ready" -> {
                BattleshipState bs = require(ctx);
                if (bs == null) return;
                if (!bs.ready(ctx.senderEmail())) {
                    ctx.replyToSender(error("Devi prima disporre una flotta valida"));
                    return;
                }
                sendStateToBoth(ctx, bs);
            }
            case "fire" -> {
                BattleshipState bs = require(ctx);
                if (bs == null) return;
                int r = payload.path("r").asInt(-1);
                int c = payload.path("c").asInt(-1);
                BattleshipState.FireResult res = bs.fire(ctx.senderEmail(), r, c);
                if (res == BattleshipState.FireResult.INVALID) {
                    ctx.replyToSender(error("Colpo non valido: turno, fase o cella già colpita"));
                    return;
                }
                sendStateToBoth(ctx, bs);
                if (bs.status() == BattleshipState.Status.WON) {
                    room.status = Room.Status.DONE;
                    sendOverToBoth(ctx, bs);
                    leaderboard.record(bs.winner(), "battleship", "WIN");
                    leaderboard.record(bs.opponent(bs.winner()), "battleship", "LOSE");
                }
            }
            default -> ctx.replyToSender(error("Azione sconosciuta: " + type));
        }
    }

    private BattleshipState require(GameContext ctx) {
        if (ctx.room().game instanceof BattleshipState bs) return bs;
        ctx.replyToSender(error("Partita non avviata"));
        return null;
    }

    /** Estrae la lista di navi {r,c,len,horizontal} dal payload; null se malformato. */
    private List<int[]> parseShips(JsonNode payload) {
        JsonNode arr = payload.path("ships");
        if (!arr.isArray()) return null;
        List<int[]> ships = new ArrayList<>();
        for (JsonNode n : arr) {
            if (!n.has("r") || !n.has("c") || !n.has("len")) return null;
            int r = n.path("r").asInt();
            int c = n.path("c").asInt();
            int len = n.path("len").asInt();
            boolean horizontal = n.path("horizontal").asBoolean(false);
            ships.add(new int[]{r, c, len, horizontal ? 1 : 0});
        }
        return ships;
    }

    /** Invia a ENTRAMBI i giocatori la loro vista personalizzata. */
    private void sendStateToBoth(GameContext ctx, BattleshipState bs) {
        ctx.sendTo(bs.player1(), viewFor(bs, bs.player1()));
        ctx.sendTo(bs.player2(), viewFor(bs, bs.player2()));
    }

    private void sendOverToBoth(GameContext ctx, BattleshipState bs) {
        ctx.sendTo(bs.player1(), over(bs, bs.player1()));
        ctx.sendTo(bs.player2(), over(bs, bs.player2()));
    }

    /** Vista personalizzata del giocatore: include la SUA board e quella avversaria mascherata. */
    private Map<String, Object> viewFor(BattleshipState bs, String player) {
        boolean won = bs.status() == BattleshipState.Status.WON;
        String status;
        if (!won) {
            status = "PLAYING";
        } else {
            status = player.equals(bs.winner()) ? "WON" : "LOST";
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:state");
        m.put("game", "battleship");
        m.put("phase", bs.phase().name());
        m.put("status", status);
        m.put("yourTurn", player.equals(bs.currentTurn()));
        m.put("youReady", bs.isReady(player));
        m.put("enemyReady", bs.isReady(bs.opponent(player)));
        m.put("winner", bs.winner());
        m.put("yourBoard", bs.viewBoardOwn(player));
        m.put("enemyBoard", bs.viewBoardEnemy(player));
        return m;
    }

    private Map<String, Object> over(BattleshipState bs, String player) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:over");
        m.put("status", player.equals(bs.winner()) ? "WON" : "LOST");
        m.put("winner", bs.winner());
        return m;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "error");
        m.put("message", msg);
        return m;
    }
}
