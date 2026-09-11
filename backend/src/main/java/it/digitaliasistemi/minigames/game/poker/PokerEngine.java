package it.digitaliasistemi.minigames.game.poker;

import com.fasterxml.jackson.databind.JsonNode;
import it.digitaliasistemi.minigames.domain.AppUser;
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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Texas Hold'em in formato Sit &amp; Go: il tavolo si popola di umani e, se l'host lo chiede,
 * di avversari IA fino a riempire i posti. Le regole stanno in {@link PokerState} (pura), le
 * decisioni dell'IA in {@link PokerBot}, i Token in {@link PokerEscrowService}.
 *
 * <p>Due monete distinte, da non confondere: le <b>fiches</b> sono la moneta del tavolo (stack
 * iniziale scelto dall'host) e non lasciano mai la partita; i <b>Token</b> sono la valuta del
 * sito, trattenuti come buyin all'avvio e pagati una volta sola a chi vince il tavolo. L'IA non
 * ha portafoglio: gioca fiches della casa e non versa nulla nel montepremi — se vince lei, il
 * montepremi resta al banco.
 *
 * <p>A differenza dei giochi a turni puri, qui servono i timer (turni che scadono, IA che pensa,
 * pausa tra le mani): come Pong e Battle City l'engine trasmette via {@link RoomChannel}, che non
 * dipende dalla connessione del mittente. Ogni mutazione dello stato è serializzata sul monitor
 * dello {@code PokerState} della stanza, perché arrivano sia dai socket sia dallo scheduler.
 */
@ApplicationScoped
public class PokerEngine implements GameEngine {

    private static final Logger LOG = Logger.getLogger(PokerEngine.class);

    /** Posti massimi al tavolo (umani + IA). Coincide col tetto di giocatori della stanza. */
    public static final int MAX_SEATS = 6;
    public static final int MIN_SEATS = 2;
    public static final int DEFAULT_HUMAN_SEATS = 2;
    public static final int DEFAULT_BOT_SEATS = 2;
    public static final int DEFAULT_CHIPS = 1000;
    private static final int MIN_CHIPS = 100;
    private static final int MAX_CHIPS = 1_000_000;
    private static final int MAX_BUYIN = 100_000;

    /** Tempo per decidere, poi si bussa (o si passa, se c'è una puntata da coprire). */
    private static final long TURN_MS = 30_000;
    private static final long BOT_MIN_MS = 700;
    private static final long BOT_JITTER_MS = 1_100;
    private static final long NEXT_HAND_MS = 4_000;
    private static final long SHOWDOWN_MS = 6_500;

    /** Nomi degli avversari IA, in tema col resto del sito. */
    private static final List<String> BOT_NAMES =
        List.of("Case", "Molly", "Wintermute", "Deckard", "Rachael", "Roy", "Pris", "Neuromante");

    @Inject LeaderboardService leaderboard;
    @Inject RoomChannel channel;
    @Inject RoomManager rooms;
    @Inject PokerEscrowService escrow;

    private final Map<String, Table> tables = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    /** Dati del tavolo che non sono regole del gioco: posta, umani, timer in corso. */
    private static final class Table {
        final PokerBot.Level level;
        final int buyin;
        final List<String> humans;
        final Random rnd = new SecureRandom();
        /** Invalida i task già programmati quando il punto di decisione cambia. */
        final AtomicInteger seq = new AtomicInteger();
        volatile ScheduledFuture<?> pending;
        volatile long turnEndsAt;
        volatile boolean finished;

        Table(PokerBot.Level level, int buyin, List<String> humans) {
            this.level = level;
            this.buyin = buyin;
            this.humans = List.copyOf(humans);
        }

        int pool() { return buyin * humans.size(); }
    }

    /** Configurazione pre-partita letta da {@code Room.options}. */
    private record Config(int humanSeats, int botSeats, int buyin, int startingChips, PokerBot.Level level) {}

    @PostConstruct
    void startScheduler() {
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "poker-timer");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    void stopScheduler() {
        tables.values().forEach(t -> {
            t.finished = true;
            cancel(t);
        });
        tables.clear();
        if (scheduler != null) scheduler.shutdownNow();
    }

    @Override public String slug() { return "poker"; }

    @Override public int maxPlayers() { return MAX_SEATS; }

    /**
     * Posti umani della stanza: li sceglie l'host: il resto del tavolo sono avversari IA.
     * È il tetto vero della stanza, quindi nessuno può sedersi al posto di un'IA prevista.
     */
    @Override
    public int maxPlayers(JsonNode options) {
        return configOf(options).humanSeats();
    }

    @Override
    public void onJoin(GameContext ctx) {
        Room room = ctx.room();
        Table t = tables.get(room.code);
        if (t == null || !(room.game instanceof PokerState st)) return;
        synchronized (st) {
            ctx.replyToSender(view(t, st, ctx.senderEmail()));
        }
    }

    @Override
    public void onLeave(GameContext ctx) {
        Room room = ctx.room();
        Table t = tables.get(room.code);
        if (t == null || t.finished || !(room.game instanceof PokerState st)) return;
        synchronized (st) {
            if (st.seat(ctx.senderEmail()) == null) return;
            st.forfeit(ctx.senderEmail());
            pump(room, t, st);
        }
    }

    @Override
    public void onMessage(GameContext ctx, String type, JsonNode payload) {
        Room room = ctx.room();
        switch (type) {
            case "game:start" -> start(ctx, room);
            case "action" -> action(ctx, room, payload);
            default -> ctx.replyToSender(error("Azione sconosciuta: " + type));
        }
    }

    // ---- Avvio ----

    private void start(GameContext ctx, Room room) {
        Table existing = tables.get(room.code);
        if (existing != null && !existing.finished) {
            ctx.replyToSender(error("Partita già in corso"));
            return;
        }

        Config cfg = configOf(room.options);
        List<String> humans = new ArrayList<>(room.players);
        // Il tetto umano è già quello della stanza; questa è la rete di sicurezza per le stanze
        // nate prima di un cambio di configurazione, che non devono sfondare il tavolo.
        if (humans.size() > MAX_SEATS) humans = humans.subList(0, MAX_SEATS);
        int bots = clamp(cfg.botSeats(), 0, MAX_SEATS - humans.size());
        if (humans.size() + bots < MIN_SEATS) {
            ctx.replyToSender(error("Servono almeno 2 al tavolo: aspetta un altro giocatore o aggiungi un avversario IA"));
            return;
        }

        if (cfg.buyin() > 0) {
            List<String> broke = escrow.whoCannotAfford(humans, cfg.buyin());
            if (!broke.isEmpty()) {
                ctx.replyToSender(error("Token insufficienti per il buyin di " + cfg.buyin()
                        + ": " + String.join(", ", broke.stream().map(this::displayName).toList())));
                return;
            }
        }

        List<PokerState.Seat> seatList = new ArrayList<>();
        for (String u : humans) seatList.add(new PokerState.Seat(u, displayName(u), false));
        List<String> names = new ArrayList<>(BOT_NAMES);
        Collections.shuffle(names, new SecureRandom());
        for (int i = 0; i < bots; i++) {
            seatList.add(new PokerState.Seat("bot:" + (i + 1), names.get(i % names.size()), true));
        }

        // Stato PLAYING prima dell'addebito: il rimborso periodico degli escrow abbandonati
        // riconosce così il tavolo come vivo e non lo intercetta appena nato.
        Room.Status previous = room.status;
        room.status = Room.Status.PLAYING;
        if (cfg.buyin() > 0) {
            try {
                escrow.collect(room.code, humans, cfg.buyin());
            } catch (RuntimeException e) {
                room.status = previous;
                LOG.warnf("Poker %s: buyin non trattenuto (%s)", room.code, e.getMessage());
                ctx.replyToSender(error("Non è stato possibile trattenere il buyin: riprova"));
                return;
            }
        }

        int smallBlind = Math.max(1, cfg.startingChips() / 100);
        PokerState st = new PokerState(seatList, cfg.startingChips(), smallBlind,
                PokerState.DEFAULT_HANDS_PER_LEVEL, new SecureRandom());
        Table t = new Table(cfg.level(), cfg.buyin(), humans);
        tables.put(room.code, t);
        room.game = st;
        synchronized (st) {
            st.startHand();
            pump(room, t, st);
        }
    }

    private Config configOf(JsonNode o) {
        if (o == null || o.isNull()) {
            return new Config(DEFAULT_HUMAN_SEATS, DEFAULT_BOT_SEATS, 0, DEFAULT_CHIPS, PokerBot.Level.NORMALE);
        }
        int humanSeats = clamp(o.path("humanSeats").asInt(DEFAULT_HUMAN_SEATS), 1, MAX_SEATS);
        int botSeats = clamp(o.path("botSeats").asInt(DEFAULT_BOT_SEATS), 0, MAX_SEATS - 1);
        int buyin = clamp(o.path("buyin").asInt(0), 0, MAX_BUYIN);
        int chips = clamp(o.path("startingChips").asInt(DEFAULT_CHIPS), MIN_CHIPS, MAX_CHIPS);
        return new Config(humanSeats, botSeats, buyin, chips, PokerBot.Level.parse(o.path("botLevel").asText("normale")));
    }

    // ---- Azioni ----

    private void action(GameContext ctx, Room room, JsonNode payload) {
        Table t = tables.get(room.code);
        if (t == null || t.finished || !(room.game instanceof PokerState st)) {
            ctx.replyToSender(error("Partita non avviata"));
            return;
        }
        PokerState.Move move = parseMove(payload.path("move").asText(""));
        if (move == null) {
            ctx.replyToSender(error("Mossa sconosciuta"));
            return;
        }
        synchronized (st) {
            String refused = st.act(ctx.senderEmail(), move, payload.path("amount").asInt(0));
            if (refused != null) {
                ctx.replyToSender(error(refused));
                return;
            }
            pump(room, t, st);
        }
    }

    private PokerState.Move parseMove(String raw) {
        return switch (raw.trim().toLowerCase()) {
            case "fold" -> PokerState.Move.FOLD;
            case "check" -> PokerState.Move.CHECK;
            case "call" -> PokerState.Move.CALL;
            case "raise" -> PokerState.Move.RAISE;
            default -> null;
        };
    }

    /**
     * Trasmette lo stato e programma il prossimo passo: turno che scade per un umano, pensata
     * dell'IA, pausa prima della mano successiva, oppure chiusura del tavolo.
     * Va chiamato con il monitor dello stato già acquisito.
     */
    private void pump(Room room, Table t, PokerState st) {
        if (st.status() == PokerState.Status.OVER) {
            sendState(room, t, st);
            finish(room, t, st);
            return;
        }
        // Nessun umano più in gioco: il tavolo si chiude subito invece di far giocare i bot fra
        // loro. Il montepremi non ha più un umano che possa vincerlo, quindi resta al banco —
        // esattamente come se il tavolo lo avesse vinto un'IA.
        if (noHumansLeft(st)) {
            sendState(room, t, st);
            finish(room, t, st);
            return;
        }
        if (st.phase() == PokerState.Phase.HAND_OVER) {
            t.turnEndsAt = 0;
            long pause = st.lastHand() != null && st.lastHand().showdown() ? SHOWDOWN_MS : NEXT_HAND_MS;
            sendState(room, t, st);
            schedule(room, t, pause, () -> {
                synchronized (st) {
                    if (st.status() == PokerState.Status.OVER) return;
                    st.startHand();
                    pump(room, t, st);
                }
            });
            return;
        }

        PokerState.Seat actor = st.seat(st.actor());
        if (actor == null) {
            sendState(room, t, st);
            return;
        }
        if (actor.bot) {
            t.turnEndsAt = 0;
            sendState(room, t, st);
            long think = BOT_MIN_MS + (long) (t.rnd.nextDouble() * BOT_JITTER_MS);
            schedule(room, t, think, () -> {
                synchronized (st) {
                    playBot(room, t, st, actor.id);
                }
            });
            return;
        }

        t.turnEndsAt = System.currentTimeMillis() + TURN_MS;
        sendState(room, t, st);
        schedule(room, t, TURN_MS, () -> {
            synchronized (st) {
                String id = st.actor();
                if (id == null || !id.equals(actor.id)) return;
                // Tempo scaduto: si bussa se è gratis, altrimenti si passa la mano.
                if (st.legalMoves(id).contains(PokerState.Move.CHECK)) st.act(id, PokerState.Move.CHECK, 0);
                else st.act(id, PokerState.Move.FOLD, 0);
                pump(room, t, st);
            }
        });
    }

    /**
     * Tutti gli umani seduti sono stati eliminati (o hanno lasciato il tavolo).
     * Si guarda {@code out}, non le fiches: un umano all-in ha lo stack a zero ma è ancora
     * in gioco, ed è solo la fine della mano che dice se è fuori davvero.
     */
    private boolean noHumansLeft(PokerState st) {
        return st.seats().stream().noneMatch(s -> !s.bot && !s.out());
    }

    private void playBot(Room room, Table t, PokerState st, String seatId) {
        if (!seatId.equals(st.actor())) return;
        PokerBot.Decision d = PokerBot.decide(st, seatId, t.level, t.rnd);
        String refused = st.act(seatId, d.move(), d.amount());
        if (refused != null) {
            // Mossa candidata non valida (bordo di regole): ripiego sulla scelta sicura.
            List<PokerState.Move> legal = st.legalMoves(seatId);
            PokerState.Move fallback = legal.contains(PokerState.Move.CHECK)
                    ? PokerState.Move.CHECK : PokerState.Move.FOLD;
            st.act(seatId, fallback, 0);
        }
        pump(room, t, st);
    }

    /** Programma un passo, annullando quello pendente: un solo timer per tavolo. */
    private void schedule(Room room, Table t, long delayMs, Runnable body) {
        cancel(t);
        int seq = t.seq.incrementAndGet();
        t.pending = scheduler.schedule(() -> RequestContexts.run(() -> {
            if (t.finished || t.seq.get() != seq) return;
            if (rooms.get(room.code) == null) return; // stanza svanita: lo escrow lo rimborsa il servizio
            try {
                body.run();
            } catch (RuntimeException e) {
                LOG.errorf(e, "Poker %s: passo non completato", room.code);
            }
        }), delayMs, TimeUnit.MILLISECONDS);
    }

    private void cancel(Table t) {
        ScheduledFuture<?> f = t.pending;
        if (f != null) f.cancel(false);
        t.pending = null;
    }

    // ---- Chiusura ----

    private void finish(Room room, Table t, PokerState st) {
        if (t.finished) return;
        t.finished = true;
        cancel(t);
        String winner = st.winner();
        // Montepremi prima di marcare DONE: così il rimborso periodico non vede mai un tavolo
        // concluso con l'escrow ancora aperto.
        int paid = t.buyin > 0 ? escrow.settle(room.code, winner) : 0;
        room.status = Room.Status.DONE;
        for (String u : t.humans) {
            leaderboard.record(u, slug(), u.equals(winner) ? "WIN" : "LOSE");
        }
        sendOver(room, t, st, paid);
        tables.remove(room.code);
    }

    private void sendOver(Room room, Table t, PokerState st, int paid) {
        List<String> order = st.finishOrder();
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            PokerState.Seat s = st.seat(order.get(i));
            if (s == null) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("place", i + 1);
            row.put("id", s.id);
            row.put("name", s.name);
            row.put("bot", s.bot);
            row.put("chips", s.chips());
            ranking.add(row);
        }
        for (String u : List.copyOf(room.players)) {
            boolean won = u.equals(st.winner());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "game:over");
            m.put("game", slug());
            m.put("status", won ? "WON" : "LOST");
            m.put("winner", st.winner());
            m.put("winnerName", st.seat(st.winner()) != null ? st.seat(st.winner()).name : null);
            m.put("ranking", ranking);
            m.put("buyin", t.buyin);
            m.put("pool", t.pool());
            m.put("tokensWon", won ? paid : 0);
            m.put("tokensLost", !won && t.humans.contains(u) ? t.buyin : 0);
            channel.sendTo(room.code, u, m);
        }
    }

    // ---- Snapshot ----

    private void sendState(Room room, Table t, PokerState st) {
        for (String u : List.copyOf(room.players)) {
            channel.sendTo(room.code, u, view(t, st, u));
        }
    }

    /**
     * Snapshot completo dal punto di vista di un giocatore: le carte coperte degli altri non
     * compaiono mai, se non tra quelle mostrate allo showdown.
     */
    private Map<String, Object> view(Table t, PokerState st, String viewer) {
        PokerState.HandResult last = st.lastHand();
        Map<String, int[]> revealed = last != null ? last.revealed() : Map.of();
        PokerState.Seat mySeat = viewer == null ? null : st.seat(viewer);

        List<Map<String, Object>> seats = new ArrayList<>();
        for (PokerState.Seat s : st.seats()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", s.id);
            row.put("name", s.name);
            row.put("bot", s.bot);
            row.put("chips", s.chips());
            row.put("bet", s.bet());
            row.put("committed", s.committed());
            row.put("folded", s.folded());
            row.put("allIn", s.allIn());
            row.put("out", s.out());
            row.put("dealer", s.id.equals(st.dealerId()));
            row.put("you", s == mySeat);
            int[] hole = s.hole();
            boolean mine = s == mySeat;
            boolean shown = revealed.containsKey(s.id);
            row.put("cards", mine || shown ? (mine ? hole : revealed.get(s.id)) : null);
            row.put("hasCards", hole.length > 0 && !s.folded());
            seats.add(row);
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:state");
        m.put("game", slug());
        m.put("status", st.status().name());
        m.put("phase", st.phase().name());
        m.put("street", st.street().name());
        m.put("handNo", st.handNo());
        m.put("pot", st.pot());
        m.put("currentBet", st.currentBet());
        m.put("smallBlind", st.smallBlind());
        m.put("bigBlind", st.bigBlind());
        m.put("board", st.board());
        m.put("dealer", st.dealerId());
        m.put("actor", st.actor());
        m.put("turnMs", t.turnEndsAt > 0 ? Math.max(0, t.turnEndsAt - System.currentTimeMillis()) : 0);
        m.put("buyin", t.buyin);
        m.put("pool", t.pool());
        m.put("startingChips", st.startingChips());
        m.put("botLevel", t.level.name().toLowerCase());
        m.put("seats", seats);
        m.put("log", st.log());
        if (last != null) m.put("lastHand", handResult(st, last));
        if (mySeat != null) m.put("you", youBlock(st, mySeat));
        return m;
    }

    private Map<String, Object> youBlock(PokerState st, PokerState.Seat me) {
        List<PokerState.Move> legal = st.legalMoves(me.id);
        Map<String, Object> you = new LinkedHashMap<>();
        you.put("seat", me.id);
        you.put("chips", me.chips());
        you.put("cards", me.hole());
        you.put("yourTurn", me.id.equals(st.actor()));
        you.put("legal", legal.stream().map(mv -> mv.name().toLowerCase()).toList());
        you.put("call", st.callAmount(me.id));
        you.put("minRaiseTo", st.minRaiseTo(me.id));
        you.put("maxRaiseTo", st.maxRaiseTo(me.id));
        return you;
    }

    private Map<String, Object> handResult(PokerState st, PokerState.HandResult last) {
        List<Map<String, Object>> awards = new ArrayList<>();
        for (PokerState.Award a : last.awards()) {
            PokerState.Seat s = st.seat(a.seatId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.seatId());
            row.put("name", s != null ? s.name : a.seatId());
            row.put("amount", a.amount());
            row.put("hand", a.handName());
            row.put("five", a.five());
            awards.add(row);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("handNo", last.handNo());
        m.put("pot", last.pot());
        m.put("showdown", last.showdown());
        m.put("awards", awards);
        m.put("busted", last.busted());
        return m;
    }

    // ---- Utilità ----

    private String displayName(String username) {
        AppUser u = AppUser.findByUsername(username);
        return u != null && u.displayName != null ? u.displayName : username;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "error");
        m.put("message", message);
        return m;
    }
}
