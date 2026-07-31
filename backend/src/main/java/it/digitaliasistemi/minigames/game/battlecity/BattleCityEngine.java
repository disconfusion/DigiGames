package it.digitaliasistemi.minigames.game.battlecity;

import com.fasterxml.jackson.databind.JsonNode;
import it.digitaliasistemi.minigames.game.GameContext;
import it.digitaliasistemi.minigames.game.GameEngine;
import it.digitaliasistemi.minigames.game.RoomChannel;
import it.digitaliasistemi.minigames.leaderboard.LeaderboardService;
import it.digitaliasistemi.minigames.rooms.Room;
import it.digitaliasistemi.minigames.rooms.RoomManager;
import it.digitaliasistemi.minigames.util.RequestContexts;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Battle City: tank su mappa a muri distruttibili, in co-op contro l'IA o in duello 1v1.
 *
 * <p>Secondo gioco realtime della piattaforma: usa lo stesso schema del Pong — un loop per stanza
 * a {@value #TICK_HZ} Hz che avanza {@link BattleCityState} e trasmette via {@link RoomChannel}
 * (il socket del mittente non serve, il loop gira su un thread di servizio). Il passo è il tempo
 * reale trascorso, limitato, e il tick si riprogramma da solo scontando il proprio costo.
 *
 * <p>Dai client arrivano solo {@code input} (direzione premuta) e {@code shoot}: il server resta
 * l'unica autorità su movimento, collisioni e vite.
 */
@ApplicationScoped
public class BattleCityEngine implements GameEngine {

    private static final Logger LOG = Logger.getLogger(BattleCityEngine.class);

    public static final int TICK_HZ = 60;
    private static final long PERIOD_US = 1_000_000L / TICK_HZ;
    private static final double DT = 1.0 / TICK_HZ;
    private static final double MAX_DT = 0.05;
    /** Snapshot completo (griglia inclusa) ogni tanto, così un client entrato a metà si riallinea. */
    private static final int FULL_EVERY_TICKS = TICK_HZ * 2;

    @Inject LeaderboardService leaderboard;
    @Inject RoomChannel channel;
    @Inject RoomManager rooms;
    @Inject BattleCityMapStore maps;

    private final Map<String, Loop> loops = new ConcurrentHashMap<>();
    /** Mappe in costruzione, per stanza: il Construction Mode vive prima della partita. */
    private final Map<String, BattleCityBuild> builds = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    private static final class Loop {
        volatile ScheduledFuture<?> future;
        volatile boolean stopped;
        long lastTick = System.nanoTime();
        long windowStart = System.nanoTime();
        int ticksInWindow;
        int tps = TICK_HZ;
        int ticks;
    }

    @PostConstruct
    void startScheduler() {
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "battlecity-loop");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    void stopScheduler() {
        loops.keySet().forEach(this::stopLoop);
        loops.clear();
        if (scheduler != null) scheduler.shutdownNow();
    }

    @Override public String slug() { return "battlecity"; }
    @Override public int maxPlayers() { return 2; }

    @Override
    public void onJoin(GameContext ctx) {
        if (ctx.room().game instanceof BattleCityState st) {
            ctx.replyToSender(fullSnapshot(st, tpsOf(ctx.room().code)));
        }
    }

    @Override
    public void onLeave(GameContext ctx) {
        Room room = ctx.room();
        if (!(room.game instanceof BattleCityState st) || st.status() != BattleCityState.Status.PLAYING) return;
        // Duello: chi abbandona perde. Co-op: la partita continua col compagno rimasto.
        if (st.mode() == BattleCityState.Mode.DUEL) {
            String other = st.players().stream().filter(p -> !p.equals(ctx.senderEmail())).findFirst().orElse(null);
            finish(room, st, other);
        }
    }

    @Override
    public void onMessage(GameContext ctx, String type, JsonNode payload) {
        Room room = ctx.room();
        switch (type) {
            case "game:start" -> {
                BattleCityState.Mode mode = modeOf(room);
                if (mode == BattleCityState.Mode.DUEL && room.players.size() < 2) {
                    ctx.replyToSender(error("Il duello richiede 2 giocatori"));
                    return;
                }
                int level = nextLevel(room, payload);
                String[] custom = customMapFor(room, payload);
                if (custom != null && !BattleCityBuild.isPlayable(custom)) {
                    ctx.replyToSender(error("Mappa non giocabile: dai punti di comparsa non si raggiunge la base"));
                    return;
                }
                BattleCityState st = new BattleCityState(mode, level, new ArrayList<>(room.players),
                        new java.util.Random(), custom);
                room.game = st;
                room.status = Room.Status.PLAYING;
                startLoop(room, st);
                channel.broadcast(room.code, fullSnapshot(st, TICK_HZ));
            }
            // ── Construction Mode: la mappa la disegnano i giocatori ────────────────
            case "build:open" -> {
                BattleCityBuild build = builds.computeIfAbsent(room.code,
                        k -> new BattleCityBuild(new ArrayList<>(room.players)));
                channel.broadcast(room.code, buildSnapshot(build));
            }
            case "build:paint" -> {
                BattleCityBuild build = builds.get(room.code);
                if (build == null) return;
                int r = payload.path("row").asInt(-1);
                int c = payload.path("col").asInt(-1);
                String tile = payload.path("tile").asText(".");
                if (build.paint(ctx.senderEmail(), r, c, tile.isEmpty() ? '.' : tile.charAt(0))) {
                    channel.broadcast(room.code, buildSnapshot(build));
                }
            }
            case "build:tool" -> {
                BattleCityBuild build = builds.get(room.code);
                if (build == null) return;
                boolean changed = switch (payload.path("tool").asText("")) {
                    case "random" -> build.fillRandom(ctx.senderEmail(), new java.util.Random());
                    case "clear" -> build.clearZone(ctx.senderEmail());
                    case "mirror" -> build.mirror(ctx.senderEmail());
                    default -> false;
                };
                if (changed) channel.broadcast(room.code, buildSnapshot(build));
            }
            case "build:ready" -> {
                BattleCityBuild build = builds.get(room.code);
                if (build == null) return;
                build.setReady(ctx.senderEmail(), payload.path("ready").asBoolean(true));
                channel.broadcast(room.code, buildSnapshot(build));
            }
            case "build:save" -> {
                BattleCityBuild build = builds.get(room.code);
                if (build == null) {
                    ctx.replyToSender(error("Nessuna mappa in costruzione"));
                    return;
                }
                String name = payload.path("name").asText("").trim();
                if (name.isEmpty()) {
                    ctx.replyToSender(error("Serve un nome per salvare la mappa"));
                    return;
                }
                String[] rows = BattleCityBuild.sanitize(build.rows());
                if (!BattleCityBuild.isPlayable(rows)) {
                    ctx.replyToSender(error("Mappa non giocabile: dai punti di comparsa non si raggiunge la base"));
                    return;
                }
                saveMap(ctx.senderEmail(), name, rows, build.players());
                ctx.replyToSender(info("Mappa \"" + name + "\" salvata nella libreria"));
            }
            case "input" -> {
                if (!(room.game instanceof BattleCityState st)) return;
                String d = payload.path("dir").asText("");
                boolean moving = payload.path("moving").asBoolean(true);
                BattleCityState.Dir dir = switch (d) {
                    case "UP" -> BattleCityState.Dir.UP;
                    case "DOWN" -> BattleCityState.Dir.DOWN;
                    case "LEFT" -> BattleCityState.Dir.LEFT;
                    case "RIGHT" -> BattleCityState.Dir.RIGHT;
                    default -> null;
                };
                st.input(ctx.senderEmail(), dir, moving && dir != null);
            }
            case "shoot" -> {
                if (room.game instanceof BattleCityState st) st.shoot(ctx.senderEmail());
            }
            default -> ctx.replyToSender(error("Azione sconosciuta: " + type));
        }
    }

    /** Modalità scelta in lobby: "duello" o co-op (default). */
    private BattleCityState.Mode modeOf(Room room) {
        String m = room.options != null ? room.options.path("mode").asText("coop") : "coop";
        return "duello".equals(m) || "duel".equals(m) ? BattleCityState.Mode.DUEL : BattleCityState.Mode.COOP;
    }

    /**
     * Livello da giocare: quello chiesto nel messaggio, altrimenti il successivo se il livello
     * precedente è stato completato (progressione), altrimenti si ripete quello corrente.
     */
    private int nextLevel(Room room, JsonNode payload) {
        int requested = payload.path("level").asInt(0);
        if (requested > 0) return requested;
        int fromOptions = room.options != null ? room.options.path("level").asInt(1) : 1;
        if (room.game instanceof BattleCityState old) {
            return old.status() == BattleCityState.Status.WON ? old.level() + 1 : old.level();
        }
        return Math.max(1, fromOptions);
    }

    /**
     * Mappa su cui giocare: quella disegnata in stanza se c'è, altrimenti quella scelta dalla
     * libreria (opzione {@code mapId}), altrimenti null = si usa il livello standard.
     */
    private String[] customMapFor(Room room, JsonNode payload) {
        BattleCityBuild build = builds.get(room.code);
        if (build != null) return BattleCityBuild.sanitize(build.rows());
        long mapId = payload.path("mapId").asLong(
                room.options != null ? room.options.path("mapId").asLong(0) : 0);
        if (mapId <= 0) return null;
        return loadLibraryMap(mapId);
    }

    /** Legge una mappa della libreria e ne segna la giocata (accesso DB dal thread del socket). */
    private String[] loadLibraryMap(long mapId) {
        final String[][] holder = new String[1][];
        RequestContexts.run(() -> holder[0] = maps.readAndCountPlay(mapId));
        return holder[0];
    }

    /** Salva la mappa appena disegnata nella libreria condivisa. */
    private void saveMap(String username, String name, String[] rows, List<String> builders) {
        RequestContexts.run(() -> maps.save(username, name, rows, builders));
    }

    /** Snapshot della mappa in costruzione (griglia, zone, chi ha finito). */
    private Map<String, Object> buildSnapshot(BattleCityBuild build) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:build");
        m.put("game", slug());
        m.putAll(build.view());
        return m;
    }

    private Map<String, Object> info(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "info");
        m.put("message", message);
        return m;
    }

    private void startLoop(Room room, BattleCityState st) {
        stopLoop(room.code);
        Loop loop = new Loop();
        loops.put(room.code, loop);
        schedule(room, st, loop, 0);
    }

    /** Come nel Pong: il tick programma il successivo scontando il tempo speso, senza raffiche. */
    private void schedule(Room room, BattleCityState st, Loop loop, long delayUs) {
        if (loop.stopped) return;
        loop.future = scheduler.schedule(() -> {
            long start = System.nanoTime();
            tick(room, st, loop);
            long workUs = (System.nanoTime() - start) / 1_000;
            schedule(room, st, loop, Math.max(1_000, PERIOD_US - workUs));
        }, delayUs, TimeUnit.MICROSECONDS);
    }

    private void stopLoop(String code) {
        if (rooms.get(code) == null) builds.remove(code);
        Loop old = loops.remove(code);
        if (old != null) {
            old.stopped = true;
            ScheduledFuture<?> f = old.future;
            if (f != null) f.cancel(false);
        }
    }

    private void tick(Room room, BattleCityState st, Loop loop) {
        try {
            if (rooms.get(room.code) == null || room.game != st) {
                stopLoop(room.code);
                return;
            }
            if (st.status() != BattleCityState.Status.PLAYING) {
                stopLoop(room.code);
                return;
            }
            // Nessuno collegato: la partita si mette in pausa invece di giocarsi da sola.
            if (!channel.hasListeners(room.code)) {
                loop.lastTick = System.nanoTime();
                return;
            }

            measure(loop);
            boolean gridChanged = st.tick(step(loop));

            if (st.status() != BattleCityState.Status.PLAYING) {
                channel.broadcast(room.code, fullSnapshot(st, loop.tps));
                finish(room, st, st.winner());
                return;
            }
            boolean full = gridChanged || loop.ticks % FULL_EVERY_TICKS == 0;
            channel.broadcast(room.code, full ? fullSnapshot(st, loop.tps) : tickSnapshot(st, loop.tps));
        } catch (Exception e) {
            LOG.errorf(e, "Battle City: errore nel loop della stanza %s", room.code);
            stopLoop(room.code);
        }
    }

    private static double step(Loop loop) {
        long now = System.nanoTime();
        double dt = (now - loop.lastTick) / 1_000_000_000.0;
        loop.lastTick = now;
        return dt <= 0 ? DT : Math.min(dt, MAX_DT);
    }

    private void measure(Loop loop) {
        loop.ticks++;
        loop.ticksInWindow++;
        long now = System.nanoTime();
        long elapsed = now - loop.windowStart;
        if (elapsed >= 1_000_000_000L) {
            loop.tps = (int) Math.round(loop.ticksInWindow * 1_000_000_000.0 / elapsed);
            loop.ticksInWindow = 0;
            loop.windowStart = now;
        }
    }

    /** Fine partita: ferma il loop, trasmette l'esito e registra il risultato. */
    private void finish(Room room, BattleCityState st, String winner) {
        stopLoop(room.code);
        room.status = Room.Status.DONE;
        channel.broadcast(room.code, over(st, winner));
        recordResult(st, winner);
    }

    /**
     * Duello: vince chi arriva a 3 colpi. Co-op: livello completato = vittoria per tutti,
     * base perduta = sconfitta per tutti (è una partita contro il gioco, come l'Impiccato del giorno).
     */
    private void recordResult(BattleCityState st, String winner) {
        RequestContexts.run(() -> {
            try {
                if (st.mode() == BattleCityState.Mode.DUEL) {
                    if (winner == null) return;
                    for (String p : st.players()) {
                        leaderboard.record(p, slug(), p.equals(winner) ? "WIN" : "LOSE");
                    }
                } else {
                    String result = st.status() == BattleCityState.Status.WON ? "WIN" : "LOSE";
                    for (String p : st.players()) leaderboard.record(p, slug(), result);
                }
            } catch (Exception e) {
                LOG.error("Battle City: impossibile registrare il risultato", e);
            }
        });
    }

    private int tpsOf(String code) {
        Loop l = loops.get(code);
        return l != null ? l.tps : TICK_HZ;
    }

    /** Snapshot completo: griglia dei muri + tutto il resto (avvio, join, muro distrutto). */
    private Map<String, Object> fullSnapshot(BattleCityState st, int tps) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:state");
        m.put("game", slug());
        m.put("dims", Map.of("field", BattleCityState.FIELD, "cell", BattleCityState.CELL,
                "tile", BattleCityState.TILE, "tank", BattleCityState.TANK,
                "bullet", BattleCityState.BULLET));
        m.put("grid", st.gridRows());
        m.put("mode", st.mode().name());
        m.put("level", st.level());
        m.put("customMap", st.customMap());
        m.put("status", st.status().name());
        m.put("winner", st.winner());
        m.putAll(dynamic(st, tps));
        return m;
    }

    /** Snapshot per tick: solo ciò che si muove. */
    private Map<String, Object> tickSnapshot(BattleCityState st, int tps) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:tick");
        m.putAll(dynamic(st, tps));
        return m;
    }

    private Map<String, Object> dynamic(BattleCityState st, int tps) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tanks", st.tankViews());
        m.put("bullets", st.bulletViews());
        m.put("powerups", st.powerUpViews());
        m.put("stats", st.playerStats());
        m.put("enemiesLeft", st.enemiesRemaining());
        m.put("frozen", st.frozen());
        m.put("shovel", st.shovelActive());
        m.put("tps", tps);
        List<BattleCityState.Event> ev = st.drainEvents();
        if (!ev.isEmpty()) {
            List<Map<String, Object>> out = new ArrayList<>(ev.size());
            for (BattleCityState.Event e : ev) {
                Map<String, Object> em = new LinkedHashMap<>();
                em.put("type", e.type());
                em.put("x", e.x());
                em.put("y", e.y());
                if (e.by() != null) em.put("by", e.by());
                out.add(em);
            }
            m.put("events", out);
        }
        return m;
    }

    private Map<String, Object> over(BattleCityState st, String winner) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:over");
        m.put("game", slug());
        m.put("status", st.status().name());
        m.put("mode", st.mode().name());
        m.put("level", st.level());
        m.put("winner", winner);
        m.put("stats", st.playerStats());
        m.put("enemiesKilled", st.enemiesKilled());
        return m;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "error");
        m.put("message", msg);
        return m;
    }
}
