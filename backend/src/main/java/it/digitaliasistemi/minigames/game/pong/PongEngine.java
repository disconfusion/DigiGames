package it.digitaliasistemi.minigames.game.pong;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import it.digitaliasistemi.minigames.game.GameContext;
import it.digitaliasistemi.minigames.game.GameEngine;
import it.digitaliasistemi.minigames.game.RoomChannel;
import it.digitaliasistemi.minigames.leaderboard.LeaderboardService;
import it.digitaliasistemi.minigames.rooms.Room;
import it.digitaliasistemi.minigames.rooms.RoomManager;
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
 * Pong 1v1 — il primo gioco <b>realtime</b> della piattaforma.
 *
 * <p>Gli altri giochi sono a turni: lo stato avanza solo quando arriva un messaggio. Qui invece
 * la partita ha un proprio orologio: un loop a {@value #TICK_HZ} Hz (uno per stanza) avanza la
 * fisica di {@link PongState} e trasmette uno snapshot per tick. Il client interpola e disegna,
 * ma non simula: il server resta l'unica autorità (dal client arriva solo la posizione desiderata
 * della racchetta, messaggio {@code aim}).
 *
 * <p>Il loop gira fuori dal ciclo richiesta/risposta, quindi non pu&ograve; usare la
 * {@code GameContext} del mittente (legata alla connessione): trasmette via {@link RoomChannel} e,
 * per registrare il risultato a fine partita, attiva a mano il contesto di richiesta CDI —
 * altrimenti Panache lancerebbe {@code ContextNotActiveException}.
 */
@ApplicationScoped
public class PongEngine implements GameEngine {

    private static final Logger LOG = Logger.getLogger(PongEngine.class);

    /** Frequenza del loop di gioco. */
    public static final int TICK_HZ = 60;
    private static final double DT = 1.0 / TICK_HZ;
    private static final long PERIOD_US = 1_000_000L / TICK_HZ;
    /** Tetto al passo di simulazione: dopo una pausa la palla non teletrasporta. */
    private static final double MAX_DT = 0.05;
    private static final int DEFAULT_POINTS = 7;

    @Inject LeaderboardService leaderboard;
    @Inject RoomChannel channel;
    @Inject RoomManager rooms;

    /** Un loop per stanza, indicizzato per codice. */
    private final Map<String, Loop> loops = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    /** Loop di una singola partita + misura dei tick effettivi al secondo (mostrata nell'HUD). */
    private static final class Loop {
        volatile ScheduledFuture<?> future;
        volatile boolean stopped;
        long windowStart = System.nanoTime();
        /** Istante del tick precedente: il passo di simulazione è il tempo reale trascorso. */
        long lastTick = System.nanoTime();
        int ticksInWindow;
        int tps = TICK_HZ;
    }

    @PostConstruct
    void startScheduler() {
        // Pool piccolo e daemon: le partite Pong in parallelo sono poche (app interna) e i thread
        // non devono trattenere la JVM allo shutdown.
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "pong-loop");
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

    @Override
    public String slug() {
        return "pong";
    }

    @Override
    public int maxPlayers() {
        return 2;
    }

    @Override
    public void onJoin(GameContext ctx) {
        if (ctx.room().game instanceof PongState ps) {
            ctx.replyToSender(fullSnapshot(ps, tpsOf(ctx.room().code)));
        }
    }

    @Override
    public void onLeave(GameContext ctx) {
        Room room = ctx.room();
        if (room.game instanceof PongState ps && ps.status() == PongState.Status.PLAYING) {
            ps.forfeit(ctx.senderEmail());
            finish(room, ps);
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
                int points = room.options != null
                        ? room.options.path("pointsToWin").asInt(DEFAULT_POINTS)
                        : DEFAULT_POINTS;
                PongState ps = new PongState(seated.get(0), seated.get(1), points);
                room.game = ps;
                room.status = Room.Status.PLAYING;
                startLoop(room, ps);
                channel.broadcast(room.code, fullSnapshot(ps, TICK_HZ));
            }
            case "aim" -> {
                if (!(room.game instanceof PongState ps)) return; // input prima dell'avvio: ignora
                ps.aim(ctx.senderEmail(), payload.path("y").asDouble(PongState.H / 2));
            }
            default -> ctx.replyToSender(error("Azione sconosciuta: " + type));
        }
    }

    /** (Ri)avvia il loop della stanza: un eventuale loop precedente viene fermato. */
    private void startLoop(Room room, PongState ps) {
        stopLoop(room.code);
        Loop loop = new Loop();
        loops.put(room.code, loop);
        schedule(room, ps, loop, 0);
    }

    /**
     * Programma il tick successivo scontando il tempo speso in questo (simulazione + invio):
     * così la cadenza resta vicina ai {@value #TICK_HZ} Hz nominali. Riprogrammarsi da soli, invece
     * di usare {@code scheduleAtFixedRate}, evita anche le raffiche di recupero dopo uno slittamento
     * (con un passo di tempo reale farebbero correre la palla più del dovuto).
     */
    private void schedule(Room room, PongState ps, Loop loop, long delayUs) {
        if (loop.stopped) return;
        loop.future = scheduler.schedule(() -> {
            long start = System.nanoTime();
            tick(room, ps, loop);
            long workUs = (System.nanoTime() - start) / 1_000;
            schedule(room, ps, loop, Math.max(1_000, PERIOD_US - workUs));
        }, delayUs, TimeUnit.MICROSECONDS);
    }

    private void stopLoop(String code) {
        Loop old = loops.remove(code);
        if (old != null) {
            old.stopped = true;
            ScheduledFuture<?> f = old.future;
            if (f != null) f.cancel(false);
        }
    }

    /** Un passo di simulazione + trasmissione. Gira sul thread {@code pong-loop}. */
    private void tick(Room room, PongState ps, Loop loop) {
        try {
            // Stanza rimossa (RoomReaper/chiusura) o partita sostituita: il loop non serve più.
            if (rooms.get(room.code) == null || room.game != ps) {
                stopLoop(room.code);
                return;
            }
            if (ps.status() != PongState.Status.PLAYING) {
                stopLoop(room.code);
                return;
            }
            // Nessuno collegato (schede chiuse, rete caduta): la partita si mette in pausa invece di
            // giocarsi da sola. Riprende al rientro; se nessuno torna, il RoomReaper toglie la stanza.
            if (!channel.hasListeners(room.code)) {
                loop.lastTick = System.nanoTime(); // alla ripresa il dt riparte da zero
                return;
            }

            measure(loop);
            boolean scored = ps.tick(step(loop));

            if (ps.status() != PongState.Status.PLAYING) {
                channel.broadcast(room.code, fullSnapshot(ps, loop.tps));
                finish(room, ps);
                return;
            }
            // A punto fatto serve lo snapshot completo (punteggio, scambio più lungo, battuta).
            channel.broadcast(room.code, scored ? fullSnapshot(ps, loop.tps) : tickSnapshot(ps, loop.tps));
        } catch (Exception e) {
            // Un'eccezione non gestita in un task schedulato lo cancellerebbe silenziosamente.
            LOG.errorf(e, "Pong: errore nel loop della stanza %s", room.code);
            stopLoop(room.code);
        }
    }

    /**
     * Passo di simulazione: tempo reale dal tick precedente, limitato a {@value #MAX_DT} s.
     * Con un periodo di {@value #TICK_HZ} Hz vale ~1/60, ma se la macchina è carica la fisica
     * resta agganciata al tempo di parete invece di rallentare.
     */
    private static double step(Loop loop) {
        long now = System.nanoTime();
        double dt = (now - loop.lastTick) / 1_000_000_000.0;
        loop.lastTick = now;
        return dt <= 0 ? DT : Math.min(dt, MAX_DT);
    }

    /** Conta i tick effettivi nell'ultimo secondo: è il "server FPS" mostrato nell'HUD. */
    private void measure(Loop loop) {
        loop.ticksInWindow++;
        long now = System.nanoTime();
        long elapsed = now - loop.windowStart;
        if (elapsed >= 1_000_000_000L) {
            loop.tps = (int) Math.round(loop.ticksInWindow * 1_000_000_000.0 / elapsed);
            loop.ticksInWindow = 0;
            loop.windowStart = now;
        }
    }

    /** Chiusura partita: ferma il loop, trasmette game:over e registra il risultato. */
    private void finish(Room room, PongState ps) {
        stopLoop(room.code);
        room.status = Room.Status.DONE;
        channel.broadcast(room.code, over(ps));
        recordResult(ps);
    }

    /**
     * Registra vittoria/sconfitta. Chiamato anche dal thread del loop, che non ha contesto di
     * richiesta CDI: lo attiva qui attorno all'accesso Panache.
     */
    private void recordResult(PongState ps) {
        String winner = ps.winner();
        if (winner == null) return;
        ManagedContext requestContext = Arc.container().requestContext();
        boolean owned = !requestContext.isActive();
        if (owned) requestContext.activate();
        try {
            for (String p : ps.seats().keySet()) {
                leaderboard.record(p, slug(), p.equals(winner) ? "WIN" : "LOSE");
            }
        } catch (Exception e) {
            LOG.errorf(e, "Pong: impossibile registrare il risultato (winner=%s)", winner);
        } finally {
            if (owned) requestContext.terminate();
        }
    }

    private int tpsOf(String code) {
        Loop l = loops.get(code);
        return l != null ? l.tps : TICK_HZ;
    }

    /**
     * Snapshot completo: dimensioni del campo, posti, punteggio, stato. Inviato all'avvio, al join
     * e a ogni punto — il client tiene le dimensioni e per i tick riceve solo i numeri che cambiano.
     */
    private Map<String, Object> fullSnapshot(PongState ps, int tps) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:state");
        m.put("game", "pong");
        m.put("dims", Map.of(
                "w", PongState.W, "h", PongState.H,
                "paddleW", PongState.PADDLE_W, "paddleH", PongState.PADDLE_H,
                "margin", PongState.PADDLE_MARGIN, "ballR", PongState.BALL_R));
        m.put("seats", ps.seats());
        m.put("pointsToWin", ps.pointsToWin());
        m.put("status", ps.status().name());
        m.put("winner", ps.winner());
        m.put("longestRally", ps.longestRally());
        m.putAll(dynamic(ps, tps));
        return m;
    }

    /** Snapshot per tick: solo i valori che cambiano 60 volte al secondo. */
    private Map<String, Object> tickSnapshot(PongState ps, int tps) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:tick");
        m.putAll(dynamic(ps, tps));
        return m;
    }

    private Map<String, Object> dynamic(PongState ps, int tps) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("b", List.of(round(ps.ballX()), round(ps.ballY())));
        m.put("p", List.of(round(ps.paddleLY()), round(ps.paddleRY())));
        m.put("s", List.of(ps.scoreL(), ps.scoreR()));
        m.put("serveIn", round(ps.serveIn()));
        m.put("rally", ps.rally());
        m.put("speed", (int) Math.round(ps.speed()));
        m.put("tps", tps);
        return m;
    }

    private Map<String, Object> over(PongState ps) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:over");
        m.put("game", "pong");
        m.put("status", ps.status().name());
        m.put("winner", ps.winner());
        m.put("s", List.of(ps.scoreL(), ps.scoreR()));
        m.put("longestRally", ps.longestRally());
        return m;
    }

    /** Un decimale: basta per il rendering e taglia il payload di ogni tick. */
    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "error");
        m.put("message", msg);
        return m;
    }
}
