package it.digitaliasistemi.minigames.game.poker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Tavolo di Texas Hold'em, in formato Sit &amp; Go a eliminazione: si gioca mano dopo mano con
 * blinds crescenti finché resta un solo stack in piedi.
 *
 * <p>Classe pura: nessun framework, nessun I/O, nessun timer — solo le regole. I turni scaduti,
 * il ritmo dei bot e la trasmissione degli snapshot sono responsabilità di {@link PokerEngine}.
 * Le fiches ({@code chips}) sono la moneta del tavolo e non hanno alcun rapporto diretto col
 * buyin in Token: la conversione avviene una volta sola, a fine partita, nell'engine.
 *
 * <p>Il piatto non è tenuto come contatore separato: è sempre la somma di quanto ogni posto ha
 * investito nella mano ({@code committed}). Così i side pot degli all-in si ricavano dai livelli
 * di investimento senza stato aggiuntivo da mantenere coerente.
 */
public class PokerState {

    public enum Status { PLAYING, OVER }

    public enum Street { PREFLOP, FLOP, TURN, RIVER }

    /** Fase del tavolo: si sta puntando, oppure la mano è chiusa e si attende la prossima. */
    public enum Phase { BETTING, HAND_OVER }

    public enum Move { FOLD, CHECK, CALL, RAISE }

    /** Quante mani a un livello di blinds prima del raddoppio. */
    public static final int DEFAULT_HANDS_PER_LEVEL = 8;
    /** Tetto ai raddoppi delle blinds: oltre è comunque all-in forzato per tutti. */
    private static final int MAX_BLIND_LEVELS = 16;
    private static final int LOG_SIZE = 14;

    /** Un posto al tavolo. {@code id} è lo username per gli umani, {@code bot:N} per l'IA. */
    public static final class Seat {
        public final String id;
        public final String name;
        public final boolean bot;
        int chips;
        int bet;
        int committed;
        int stackAtHandStart;
        boolean folded;
        boolean allIn;
        boolean out;
        boolean acted;
        int[] hole = new int[0];

        public Seat(String id, String name, boolean bot) {
            this.id = id;
            this.name = name;
            this.bot = bot;
        }

        public int chips() { return chips; }
        public int bet() { return bet; }
        public int committed() { return committed; }
        public boolean folded() { return folded; }
        public boolean allIn() { return allIn; }
        public boolean out() { return out; }
        public int[] hole() { return hole.clone(); }
        /** Il posto è ancora in gioco nel torneo (non eliminato). */
        public boolean alive() { return !out && chips > 0; }
        /** Il posto può ancora compiere un'azione in questa mano. */
        public boolean canAct() { return !out && !folded && !allIn && chips > 0; }
    }

    /** Piatto (o side pot) assegnato a un posto, con il nome della mano che l'ha vinto. */
    public record Award(String seatId, int amount, String handName, int[] five) {}

    /**
     * Esito della mano appena conclusa: quanto valeva il piatto, chi ha vinto cosa, le carte
     * mostrate, chi è uscito. Serve alla UI, perché a mano chiusa il piatto è già stato svuotato.
     */
    public record HandResult(int handNo, int pot, List<Award> awards, boolean showdown,
                             Map<String, int[]> revealed, List<String> busted) {}

    private final List<Seat> seats;
    private final int startingChips;
    private final int baseSmallBlind;
    private final int handsPerLevel;
    private final Random rnd;

    private final List<Integer> deck = new ArrayList<>(PokerHands.DECK);
    private final int[] board = new int[5];
    private int boardCount;

    private int handNo;
    private int dealer = -1;
    private Street street = Street.PREFLOP;
    private Phase phase = Phase.HAND_OVER;
    private Status status = Status.PLAYING;
    private int currentBet;
    private int minRaise;
    private int actor = -1;
    private String winner;
    private final List<String> bustOrder = new ArrayList<>();
    private final List<String> log = new ArrayList<>();
    private HandResult lastHand;

    public PokerState(List<Seat> seats, int startingChips, int smallBlind, int handsPerLevel) {
        this(seats, startingChips, smallBlind, handsPerLevel, new Random());
    }

    public PokerState(List<Seat> seats, int startingChips, int smallBlind, int handsPerLevel, Random rnd) {
        if (seats == null || seats.size() < 2) throw new IllegalArgumentException("Servono almeno 2 posti");
        this.seats = List.copyOf(seats);
        this.startingChips = Math.max(2, startingChips);
        this.baseSmallBlind = Math.max(1, smallBlind);
        this.handsPerLevel = Math.max(1, handsPerLevel);
        this.rnd = rnd;
        for (Seat s : this.seats) s.chips = this.startingChips;
    }

    // ---- Lettura ----

    public List<Seat> seats() { return seats; }

    public Seat seat(String id) {
        for (Seat s : seats) if (s.id.equals(id)) return s;
        return null;
    }

    public Status status() { return status; }

    public Phase phase() { return phase; }

    public Street street() { return street; }

    public int handNo() { return handNo; }

    public int startingChips() { return startingChips; }

    public String winner() { return winner; }

    public HandResult lastHand() { return lastHand; }

    public List<String> log() { return List.copyOf(log); }

    /** Carte comuni girate finora (0, 3, 4 o 5). */
    public int[] board() {
        int[] out = new int[boardCount];
        System.arraycopy(board, 0, out, 0, boardCount);
        return out;
    }

    /** Piatto totale della mano: tutto ciò che è stato investito, puntate correnti incluse. */
    public int pot() {
        int total = 0;
        for (Seat s : seats) total += s.committed;
        return total;
    }

    public int currentBet() { return currentBet; }

    public int minRaise() { return minRaise; }

    /** Id del posto di turno, null se non si sta puntando. */
    public String actor() {
        return phase == Phase.BETTING && actor >= 0 ? seats.get(actor).id : null;
    }

    /** Id del posto col bottone, null prima della prima mano. */
    public String dealerId() { return dealer >= 0 ? seats.get(dealer).id : null; }

    public int smallBlind() {
        int level = Math.min((Math.max(1, handNo) - 1) / handsPerLevel, MAX_BLIND_LEVELS);
        return baseSmallBlind << level;
    }

    public int bigBlind() { return smallBlind() * 2; }

    /** Quanto deve mettere il posto per stare al gioco (limitato alle sue fiches). */
    public int callAmount(String id) {
        Seat s = seat(id);
        if (s == null) return 0;
        return Math.min(currentBet - s.bet, s.chips);
    }

    /** Puntata totale minima per rilanciare, oppure -1 se il rilancio non è possibile. */
    public int minRaiseTo(String id) {
        Seat s = seat(id);
        if (s == null || !s.canAct()) return -1;
        int max = s.bet + s.chips;
        if (max <= currentBet) return -1; // può solo stare al gioco (all-in corto) o passare
        return Math.min(currentBet + minRaise, max);
    }

    /** Puntata totale massima (all-in) del posto. */
    public int maxRaiseTo(String id) {
        Seat s = seat(id);
        return s == null ? 0 : s.bet + s.chips;
    }

    /** Mosse consentite al posto in questo momento. */
    public List<Move> legalMoves(String id) {
        Seat s = seat(id);
        if (phase != Phase.BETTING || s == null || !id.equals(actor()) || !s.canAct()) return List.of();
        List<Move> out = new ArrayList<>();
        out.add(Move.FOLD);
        if (s.bet == currentBet) out.add(Move.CHECK);
        else out.add(Move.CALL);
        if (minRaiseTo(id) > 0) out.add(Move.RAISE);
        return out;
    }

    /**
     * Ordine di arrivo finale, dal primo all'ultimo: il vincitore, poi gli eliminati dall'ultimo
     * al primo. A partita in corso contiene solo chi è già uscito.
     */
    public List<String> finishOrder() {
        List<String> out = new ArrayList<>();
        if (winner != null) out.add(winner);
        // Tavolo chiuso prima di avere un vincitore (nessun umano più in gioco): in testa
        // restano tutti quelli non ancora eliminati, all-in compresi.
        else for (Seat s : seats) if (!s.out) out.add(s.id);
        List<String> busted = new ArrayList<>(bustOrder);
        Collections.reverse(busted);
        out.addAll(busted);
        return out;
    }

    // ---- Svolgimento ----

    /** Avvia una nuova mano: bottone, carte, blinds. Non fa nulla se il torneo è finito. */
    public void startHand() {
        if (status == Status.OVER) return;
        List<Seat> alive = seats.stream().filter(Seat::alive).toList();
        if (alive.size() < 2) {
            finishTournament();
            return;
        }

        handNo++;
        shuffle();
        boardCount = 0;
        street = Street.PREFLOP;
        phase = Phase.BETTING;
        lastHand = null;
        for (Seat s : seats) {
            s.bet = 0;
            s.committed = 0;
            s.acted = false;
            s.folded = !s.alive();
            s.allIn = false;
            s.stackAtHandStart = s.chips;
            s.hole = s.alive() ? new int[]{ draw(), draw() } : new int[0];
        }

        dealer = nextAlive(dealer);
        int sb = smallBlind();
        int bb = bigBlind();
        boolean headsUp = alive.size() == 2;
        // Heads-up: il bottone è anche piccolo buio e parla per primo preflop.
        int sbSeat = headsUp ? dealer : nextAlive(dealer);
        int bbSeat = nextAlive(sbSeat);
        postBlind(seats.get(sbSeat), sb, "piccolo buio");
        postBlind(seats.get(bbSeat), bb, "grande buio");

        currentBet = bb;
        minRaise = bb;
        actor = nextToAct(bbSeat);
        note("Mano " + handNo + " · buio " + sb + "/" + bb);
        // Blinds che mettono tutti all-in: non resta nessuna decisione, si gira fino al river.
        if (!bettingOpen()) runOutAndSettle();
    }

    /**
     * Applica una mossa. Ritorna {@code null} se accettata, altrimenti il motivo del rifiuto
     * (il server è l'autorità: nessuna mossa non valida modifica lo stato).
     *
     * @param amount per {@link Move#RAISE} è la puntata <b>totale</b> sulla strada corrente
     *               ("rilancio a"), ignorato per le altre mosse.
     */
    public String act(String id, Move move, int amount) {
        if (status == Status.OVER) return "Partita conclusa";
        if (phase != Phase.BETTING) return "Mano non in corso";
        Seat s = seat(id);
        if (s == null) return "Non sei al tavolo";
        if (!id.equals(actor())) return "Non è il tuo turno";
        if (!s.canAct()) return "Non puoi agire in questa mano";

        switch (move) {
            case FOLD -> {
                s.folded = true;
                note(s.name + " passa");
            }
            case CHECK -> {
                if (s.bet != currentBet) return "C'è una puntata da coprire: stai al gioco o passa";
                note(s.name + " bussa");
            }
            case CALL -> {
                if (s.bet == currentBet) return "Niente da coprire: puoi bussare";
                int paid = pay(s, currentBet - s.bet);
                note(s.name + (s.allIn ? " va all-in per " + paid : " sta al gioco per " + paid));
            }
            case RAISE -> {
                int max = s.bet + s.chips;
                if (max <= currentBet) return "Fiches insufficienti per rilanciare";
                if (amount > max) return "Non hai abbastanza fiches";
                if (amount <= currentBet) return "Il rilancio deve superare la puntata corrente";
                if (amount < currentBet + minRaise && amount != max) {
                    return "Rilancio minimo a " + (currentBet + minRaise);
                }
                int raiseSize = amount - currentBet;
                pay(s, amount - s.bet);
                minRaise = Math.max(minRaise, raiseSize);
                currentBet = amount;
                // Il rilancio riapre l'azione: chi aveva già parlato deve rispondere.
                for (Seat other : seats) if (other != s && other.canAct()) other.acted = false;
                note(s.name + (s.allIn ? " va all-in a " + amount : " rilancia a " + amount));
            }
        }
        s.acted = true;
        afterAction();
        return null;
    }

    /**
     * Abbandono del tavolo: il posto passa la mano ed è eliminato dal torneo. Le fiches già
     * investite restano nel piatto, il resto del suo stack esce dal gioco con lui.
     */
    public void forfeit(String id) {
        Seat s = seat(id);
        if (s == null || s.out) return;
        boolean wasActing = id.equals(actor());
        note(s.name + " lascia il tavolo");
        s.folded = true;
        s.acted = true;
        s.allIn = false;
        s.chips = 0;
        s.out = true;
        if (!bustOrder.contains(id)) bustOrder.add(id);

        if (phase == Phase.BETTING) {
            if (wasActing) afterAction();
            else reviewAfterLeave();
        }
        if (status == Status.PLAYING && seats.stream().filter(Seat::alive).count() < 2) finishTournament();
    }

    // ---- Motore interno ----

    /** Dopo l'azione del posto di turno: chiude la mano, chiude la strada o passa il turno. */
    private void afterAction() {
        if (contenders().size() <= 1) {
            settle(false);
            return;
        }
        if (!bettingOpen()) {
            closeStreetAndContinue();
            return;
        }
        int next = nextToAct(actor);
        if (next < 0) closeStreetAndContinue();
        else actor = next;
    }

    /** Dopo l'abbandono di chi NON era di turno: il turno corrente non va toccato. */
    private void reviewAfterLeave() {
        if (contenders().size() <= 1) {
            settle(false);
            return;
        }
        if (!bettingOpen()) closeStreetAndContinue();
    }

    /**
     * C'è ancora una decisione da prendere su questa strada?
     *
     * <p>Non basta contare chi può puntare: se un solo posto ha fiches ma deve coprire l'all-in
     * di un altro, la sua scelta (stare al gioco o passare) è l'ultima azione della mano e va
     * chiesta. Viceversa, quando nessuno deve coprire e resta un solo posto capace di puntare,
     * puntare non avrebbe interlocutori: la strada è chiusa.
     */
    private boolean bettingOpen() {
        for (Seat s : seats) {
            if (s.canAct() && s.bet < currentBet) return true;
        }
        return anyoneCanBet() && !bettingClosed();
    }

    /** Passa alla strada successiva; se nessuno può più puntare, serve tutto e paga. */
    private void closeStreetAndContinue() {
        while (true) {
            if (street == Street.RIVER) {
                settle(true);
                return;
            }
            dealNextStreet();
            for (Seat s : seats) {
                s.bet = 0;
                s.acted = false;
            }
            currentBet = 0;
            minRaise = bigBlind();
            int first = nextToAct(dealer);
            if (first >= 0 && anyoneCanBet()) {
                actor = first;
                return;
            }
        }
    }

    /** Tutti all-in: gira le carte restanti e paga. */
    private void runOutAndSettle() {
        while (street != Street.RIVER) dealNextStreet();
        settle(true);
    }

    private void dealNextStreet() {
        switch (street) {
            case PREFLOP -> {
                draw(); // carta bruciata, come al tavolo vero
                board[0] = draw();
                board[1] = draw();
                board[2] = draw();
                boardCount = 3;
                street = Street.FLOP;
            }
            case FLOP -> {
                draw();
                board[3] = draw();
                boardCount = 4;
                street = Street.TURN;
            }
            case TURN -> {
                draw();
                board[4] = draw();
                boardCount = 5;
                street = Street.RIVER;
            }
            case RIVER -> { }
        }
    }

    private boolean bettingClosed() {
        for (Seat s : seats) {
            if (!s.canAct() || s.folded) continue;
            if (!s.acted || s.bet != currentBet) return false;
        }
        return true;
    }

    /** Almeno due posti possono ancora puntare: sotto questa soglia non c'è più azione. */
    private boolean anyoneCanBet() {
        return seats.stream().filter(Seat::canAct).count() >= 2;
    }

    /** Posti ancora in corsa per il piatto (non passati, con qualcosa investito o fiches). */
    private List<Seat> contenders() {
        return seats.stream().filter(s -> !s.folded && !s.out).toList();
    }

    private int nextAlive(int from) {
        int n = seats.size();
        for (int i = 1; i <= n; i++) {
            int idx = ((from + i) % n + n) % n;
            if (seats.get(idx).alive()) return idx;
        }
        return from;
    }

    private int nextToAct(int from) {
        int n = seats.size();
        for (int i = 1; i <= n; i++) {
            int idx = ((from + i) % n + n) % n;
            if (seats.get(idx).canAct()) return idx;
        }
        return -1;
    }

    private void postBlind(Seat s, int amount, String label) {
        int paid = pay(s, amount);
        note(s.name + ": " + label + " " + paid);
    }

    /** Sposta fiches dal posto al piatto, limitate allo stack. Ritorna quanto pagato. */
    private int pay(Seat s, int amount) {
        int paid = Math.min(amount, s.chips);
        s.chips -= paid;
        s.bet += paid;
        s.committed += paid;
        if (s.chips == 0) s.allIn = true;
        return paid;
    }

    /** Assegna il piatto (con i side pot), elimina chi è a zero e chiude la mano. */
    private void settle(boolean showdown) {
        List<Seat> contenders = contenders();
        List<Award> awards = new ArrayList<>();
        // Il piatto va letto ora: alla fine gli investimenti vengono azzerati, così le fiches
        // assegnate non risultano contate due volte (negli stack e ancora nel piatto).
        final int potTotal = pot();
        int distributed = 0;

        if (contenders.isEmpty()) {
            // Tutti hanno abbandonato il tavolo nella stessa mano: niente da assegnare.
            clearBets();
            phase = Phase.HAND_OVER;
            actor = -1;
            lastHand = new HandResult(handNo, potTotal, List.of(), false, Map.of(), List.of());
            finishTournament();
            return;
        }

        if (contenders.size() == 1) {
            Seat s = contenders.get(0);
            int amount = potTotal;
            s.chips += amount;
            distributed = amount;
            awards.add(new Award(s.id, amount, null, new int[0]));
            note(s.name + " vince " + amount + " (nessuno ha voluto vedere)");
        } else {
            Map<String, Integer> won = new LinkedHashMap<>();
            Map<String, int[]> five = new LinkedHashMap<>();
            Map<String, Integer> scores = new LinkedHashMap<>();
            for (Seat s : contenders) {
                int[] cards = withBoard(s.hole);
                scores.put(s.id, PokerHands.best(cards));
                five.put(s.id, PokerHands.bestFive(cards));
            }

            int[] levels = contenders.stream().mapToInt(s -> s.committed).distinct().sorted().toArray();
            int prev = 0;
            List<Seat> lastEligible = List.of();
            for (int level : levels) {
                int amount = 0;
                for (Seat s : seats) {
                    amount += Math.min(s.committed, level) - Math.min(s.committed, prev);
                }
                prev = level;
                if (amount <= 0) continue;
                final int cap = level;
                List<Seat> eligible = contenders.stream().filter(s -> s.committed >= cap).toList();
                lastEligible = eligible;
                distributed += amount;
                for (var e : splitPot(amount, eligible, scores).entrySet()) {
                    won.merge(e.getKey(), e.getValue(), Integer::sum);
                }
            }

            // Fiches investite da chi ha passato oltre il livello del contendente più ricco:
            // restano nel piatto e vanno ai vincitori dell'ultimo livello (nessuna fiche svanisce).
            int leftover = potTotal - distributed;
            if (leftover > 0 && !lastEligible.isEmpty()) {
                distributed += leftover;
                for (var e : splitPot(leftover, lastEligible, scores).entrySet()) {
                    won.merge(e.getKey(), e.getValue(), Integer::sum);
                }
            }

            for (var e : won.entrySet()) {
                Seat s = seat(e.getKey());
                s.chips += e.getValue();
                String handName = PokerHands.categoryName(scores.get(s.id));
                awards.add(new Award(s.id, e.getValue(), handName, five.get(s.id)));
                note(s.name + " vince " + e.getValue() + " con " + handName.toLowerCase());
            }
        }

        Map<String, int[]> revealed = new LinkedHashMap<>();
        if (showdown && contenders.size() > 1) {
            for (Seat s : contenders) revealed.put(s.id, s.hole());
        }

        // Eliminazioni: a pari mano, chi era partito con meno fiches finisce più in basso.
        List<Seat> busted = new ArrayList<>(seats.stream().filter(s -> !s.out && s.chips <= 0).toList());
        busted.sort((a, b) -> Integer.compare(a.stackAtHandStart, b.stackAtHandStart));
        List<String> bustedIds = new ArrayList<>();
        for (Seat s : busted) {
            s.out = true;
            s.chips = 0;
            bustOrder.add(s.id);
            bustedIds.add(s.id);
            note(s.name + " è fuori");
        }

        clearBets();
        phase = Phase.HAND_OVER;
        actor = -1;
        lastHand = new HandResult(handNo, potTotal, awards,
                showdown && contenders.size() > 1, revealed, bustedIds);
        if (seats.stream().filter(Seat::alive).count() < 2) finishTournament();
    }

    /** Divide un piatto tra i migliori punteggi, con le fiches di resto ai primi dopo il bottone. */
    private Map<String, Integer> splitPot(int amount, List<Seat> eligible, Map<String, Integer> scores) {
        int best = eligible.stream().mapToInt(s -> scores.get(s.id)).max().orElse(-1);
        // Ordine dei vincitori a partire dal posto dopo il bottone: è il criterio del tavolo
        // per le fiches di resto (odd chip), così resta deterministico e non arbitrario.
        Set<String> winners = new LinkedHashSet<>();
        int n = seats.size();
        for (int i = 1; i <= n; i++) {
            Seat s = seats.get(((dealer + i) % n + n) % n);
            if (eligible.contains(s) && scores.get(s.id) == best) winners.add(s.id);
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        int share = amount / winners.size();
        int remainder = amount % winners.size();
        for (String id : winners) {
            int extra = remainder-- > 0 ? 1 : 0;
            out.put(id, share + extra);
        }
        return out;
    }

    /** Svuota il piatto: la mano è chiusa e le fiches sono già passate negli stack. */
    private void clearBets() {
        for (Seat s : seats) {
            s.bet = 0;
            s.committed = 0;
        }
    }

    private void finishTournament() {
        status = Status.OVER;
        phase = Phase.HAND_OVER;
        actor = -1;
        winner = seats.stream().filter(Seat::alive).map(s -> s.id).findFirst().orElse(null);
        if (winner != null) {
            note(seat(winner).name + " vince il tavolo");
            for (Seat s : seats) {
                if (!s.id.equals(winner) && !bustOrder.contains(s.id)) bustOrder.add(s.id);
            }
        }
    }

    private int[] withBoard(int[] hole) {
        int[] out = new int[hole.length + boardCount];
        System.arraycopy(hole, 0, out, 0, hole.length);
        System.arraycopy(board, 0, out, hole.length, boardCount);
        return out;
    }

    private void shuffle() {
        deck.clear();
        for (int c = 0; c < PokerHands.DECK; c++) deck.add(c);
        Collections.shuffle(deck, rnd);
    }

    private int draw() {
        return deck.remove(deck.size() - 1);
    }

    private void note(String line) {
        log.add(line);
        while (log.size() > LOG_SIZE) log.remove(0);
    }
}
