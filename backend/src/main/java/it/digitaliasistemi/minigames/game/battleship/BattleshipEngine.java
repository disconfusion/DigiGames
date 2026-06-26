package it.digitaliasistemi.minigames.game.battleship;

import com.fasterxml.jackson.databind.JsonNode;
import it.digitaliasistemi.minigames.game.GameContext;
import it.digitaliasistemi.minigames.game.GameEngine;
import it.digitaliasistemi.minigames.leaderboard.LeaderboardService;
import it.digitaliasistemi.minigames.rooms.Room;
import it.digitaliasistemi.minigames.shop.InventoryService;
import it.digitaliasistemi.minigames.shop.PowerCatalog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Battaglia Navale con poteri (cyberdeck). 2 giocatori, informazione nascosta.
 * Snapshot per-giocatore via {@link GameContext#sendTo}: lo stato non va mai in broadcast.
 */
@ApplicationScoped
public class BattleshipEngine implements GameEngine {

    @Inject LeaderboardService leaderboard;
    @Inject InventoryService inventory;

    @Override public String slug() { return "battleship"; }

    @Override public int maxPlayers() { return 2; }

    @Override
    public void onJoin(GameContext ctx) {
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
                finishIfWon(ctx, bs);
            }
            case "power:use" -> usePower(ctx, payload);
            default -> ctx.replyToSender(error("Azione sconosciuta: " + type));
        }
    }

    // ---- Poteri ----

    private void usePower(GameContext ctx, JsonNode payload) {
        BattleshipState bs = require(ctx);
        if (bs == null) return;
        String user = ctx.senderEmail();
        String powerId = payload.path("powerId").asText("");
        var defOpt = PowerCatalog.byId(powerId);
        if (defOpt.isEmpty() || !"battleship".equals(defOpt.get().game())) {
            ctx.replyToSender(error("Potere non valido"));
            return;
        }
        if (inventory.quantity(user, powerId) <= 0) {
            ctx.replyToSender(error("Non possiedi questo potere"));
            return;
        }

        boolean applied = false;
        switch (powerId) {
            case "bs_torpedo" -> {
                int r = payload.path("r").asInt(-1);
                int c = payload.path("c").asInt(-1);
                BattleshipState.FireOutcome out = bs.fireProximity(user, r, c);
                if (out.result() == BattleshipState.FireResult.INVALID) {
                    ctx.replyToSender(error("Siluro non valido: turno, fase o cella già colpita"));
                    return;
                }
                applied = true;
                ctx.replyToSender(torpedoEvent(r, c, out));
                sendStateToBoth(ctx, bs);
                finishIfWon(ctx, bs);
            }
            case "bs_radar" -> {
                int r = payload.path("r").asInt(-1);
                int c = payload.path("c").asInt(-1);
                List<int[]> cells = bs.radarPeek(user, r, c);
                if (cells.isEmpty()) {
                    ctx.replyToSender(error("Radar non utilizzabile qui"));
                    return;
                }
                applied = true;
                ctx.replyToSender(radarEvent(cells));
                ctx.replyToSender(viewFor(bs, user)); // aggiorna le cariche nel cyberdeck
            }
            case "bs_move_ship" -> {
                int fromR = payload.path("fromR").asInt(-1);
                int fromC = payload.path("fromC").asInt(-1);
                int toR = payload.path("toR").asInt(-1);
                int toC = payload.path("toC").asInt(-1);
                boolean horizontal = payload.path("horizontal").asBoolean(true);
                if (!bs.moveShip(user, fromR, fromC, toR, toC, horizontal)) {
                    ctx.replyToSender(error("Spostamento non valido"));
                    return;
                }
                applied = true;
                sendStateToBoth(ctx, bs);
            }
            case "bs_extend_ship" -> {
                int r = payload.path("r").asInt(-1);
                int c = payload.path("c").asInt(-1);
                if (!bs.extendShip(user, r, c)) {
                    ctx.replyToSender(error("Impossibile allungare questa nave"));
                    return;
                }
                applied = true;
                sendStateToBoth(ctx, bs);
            }
            case "bs_expand_board" -> {
                if (!bs.expandBoard(user)) {
                    ctx.replyToSender(error("Impossibile espandere il tabellone"));
                    return;
                }
                applied = true;
                sendStateToBoth(ctx, bs);
            }
            case "bs_extra_ship" -> {
                if (!bs.addExtraShip(user)) {
                    ctx.replyToSender(error("Nessuno spazio per una nave extra"));
                    return;
                }
                applied = true;
                sendStateToBoth(ctx, bs);
            }
            case "bs_decoy" -> {
                int r = payload.path("r").asInt(-1);
                int c = payload.path("c").asInt(-1);
                if (!bs.placeDecoy(user, r, c)) {
                    ctx.replyToSender(error("L'esca va piazzata su una tua nave"));
                    return;
                }
                applied = true;
                sendStateToBoth(ctx, bs);
            }
            default -> {
                ctx.replyToSender(error("Potere sconosciuto"));
                return;
            }
        }
        if (applied) inventory.consume(user, powerId);
    }

    private void finishIfWon(GameContext ctx, BattleshipState bs) {
        if (bs.status() == BattleshipState.Status.WON) {
            ctx.room().status = Room.Status.DONE;
            sendOverToBoth(ctx, bs);
            leaderboard.record(bs.winner(), "battleship", "WIN");
            leaderboard.record(bs.opponent(bs.winner()), "battleship", "LOSE");
        }
    }

    // ---- Helper ----

    private BattleshipState require(GameContext ctx) {
        if (ctx.room().game instanceof BattleshipState bs) return bs;
        ctx.replyToSender(error("Partita non avviata"));
        return null;
    }

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

    private void sendStateToBoth(GameContext ctx, BattleshipState bs) {
        ctx.sendTo(bs.player1(), viewFor(bs, bs.player1()));
        ctx.sendTo(bs.player2(), viewFor(bs, bs.player2()));
    }

    private void sendOverToBoth(GameContext ctx, BattleshipState bs) {
        ctx.sendTo(bs.player1(), over(bs, bs.player1()));
        ctx.sendTo(bs.player2(), over(bs, bs.player2()));
    }

    /** Vista personalizzata: propria board, board avversaria mascherata, esche, cyberdeck. */
    private Map<String, Object> viewFor(BattleshipState bs, String player) {
        boolean won = bs.status() == BattleshipState.Status.WON;
        String status = !won ? "PLAYING" : (player.equals(bs.winner()) ? "WON" : "LOST");
        String enemy = bs.opponent(player);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:state");
        m.put("game", "battleship");
        m.put("phase", bs.phase().name());
        m.put("status", status);
        m.put("yourTurn", player.equals(bs.currentTurn()));
        m.put("youReady", bs.isReady(player));
        m.put("enemyReady", bs.isReady(enemy));
        m.put("winner", bs.winner());
        m.put("cols", BattleshipState.COLS);
        m.put("yourRows", bs.rowsOf(player));
        m.put("enemyRows", bs.rowsOf(enemy));
        m.put("yourBoard", bs.viewBoardOwn(player));
        m.put("enemyBoard", bs.viewBoardEnemy(player));
        m.put("yourFleet", bs.fleetCount(player));
        m.put("enemyFleet", bs.fleetCount(enemy));
        m.put("yourSunk", bs.sunkCount(player));
        m.put("enemySunk", bs.sunkCount(enemy));
        m.put("decoys", bs.decoyCells(player));
        m.put("powers", buildPowers(player));
        return m;
    }

    /** Cyberdeck: poteri della battaglia navale con metadati e cariche possedute dall'utente. */
    private List<Map<String, Object>> buildPowers(String player) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (PowerCatalog.PowerDef d : PowerCatalog.forGame("battleship")) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", d.id());
            p.put("label", d.label());
            p.put("emoji", d.emoji());
            p.put("usage", d.usage());
            p.put("phase", d.phase());
            p.put("owned", inventory.quantity(player, d.id()));
            out.add(p);
        }
        return out;
    }

    private Map<String, Object> torpedoEvent(int r, int c, BattleshipState.FireOutcome out) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "power:torpedo");
        m.put("r", r);
        m.put("c", c);
        m.put("result", out.result().name());
        m.put("proximity", out.proximity());
        return m;
    }

    private Map<String, Object> radarEvent(List<int[]> cells) {
        List<List<Integer>> list = new ArrayList<>();
        for (int[] cell : cells) list.add(List.of(cell[0], cell[1], cell[2]));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "power:radar");
        m.put("cells", list);
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
