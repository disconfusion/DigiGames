package it.digitaliasistemi.minigames.game.poker;

import java.util.Random;

/**
 * Avversari IA del tavolo: decidono la mossa dalla forza della mano confrontata con le quote
 * del piatto, con una dose di rumore e di bluff perché non siano leggibili a memoria.
 *
 * <p>Classe pura come {@link PokerState}: nessuno stato, nessun framework. Il bot legge lo stato
 * del tavolo (lato server ha legittimamente accesso alle proprie carte) e ritorna una mossa
 * <b>candidata</b>: l'autorità resta a {@link PokerState#act}, che la rifiuta se non è valida —
 * l'engine in quel caso ripiega su bussata o passo.
 *
 * <p>La forza postflop è una stima per categoria di mano, non un calcolo di equity: per un
 * minigioco tra colleghi regge il confronto, ma non è un solver.
 */
public final class PokerBot {

    /** Quanto è tosto l'avversario: sceglie soglie di call, aggressività e frequenza di bluff. */
    public enum Level {
        FACILE(0.35, -0.14, 0.03),
        NORMALE(0.70, 0.00, 0.07),
        TOSTA(1.00, 0.06, 0.13);

        /** Probabilità di trasformare una mano forte in rilancio invece che in semplice call. */
        final double aggression;
        /** Margine aggiunto alle quote del piatto per decidere se vale la pena pagare. */
        final double callMargin;
        final double bluffRate;

        Level(double aggression, double callMargin, double bluffRate) {
            this.aggression = aggression;
            this.callMargin = callMargin;
            this.bluffRate = bluffRate;
        }

        public static Level parse(String raw) {
            if (raw == null) return NORMALE;
            return switch (raw.trim().toLowerCase()) {
                case "facile" -> FACILE;
                case "tosta", "difficile" -> TOSTA;
                default -> NORMALE;
            };
        }
    }

    /** Mossa proposta: {@code amount} vale solo per il rilancio (puntata totale sulla strada). */
    public record Decision(PokerState.Move move, int amount) {}

    private PokerBot() {}

    public static Decision decide(PokerState st, String seatId, Level level, Random rnd) {
        PokerState.Seat me = st.seat(seatId);
        if (me == null) return new Decision(PokerState.Move.FOLD, 0);

        int call = st.callAmount(seatId);
        int pot = st.pot();
        int minRaiseTo = st.minRaiseTo(seatId);
        int maxRaiseTo = st.maxRaiseTo(seatId);
        boolean canRaise = minRaiseTo > 0;

        // Rumore sulla stima: due mani identiche non vengono giocate sempre allo stesso modo.
        double strength = clamp(strength(st, me) + (rnd.nextDouble() - 0.5) * 0.08);

        if (call <= 0) {
            boolean value = strength > 0.70 && rnd.nextDouble() < level.aggression;
            boolean bluff = strength < 0.45 && rnd.nextDouble() < level.bluffRate;
            if (canRaise && (value || bluff)) {
                return new Decision(PokerState.Move.RAISE,
                        raiseTo(st, pot, strength, me, minRaiseTo, maxRaiseTo, rnd));
            }
            return new Decision(PokerState.Move.CHECK, 0);
        }

        double potOdds = (double) call / (pot + call);
        if (strength > 0.82 && canRaise && rnd.nextDouble() < level.aggression) {
            return new Decision(PokerState.Move.RAISE,
                    raiseTo(st, pot, strength, me, minRaiseTo, maxRaiseTo, rnd));
        }
        if (strength >= potOdds + level.callMargin) {
            return new Decision(PokerState.Move.CALL, 0);
        }
        // Bluff-call sporadico: tiene onesti gli avversari che puntano su ogni piatto.
        if (rnd.nextDouble() < level.bluffRate && call <= Math.max(1, pot / 4)) {
            return new Decision(PokerState.Move.CALL, 0);
        }
        return new Decision(PokerState.Move.FOLD, 0);
    }

    /** Dimensiona il rilancio in frazione di piatto, con shove quando lo stack è già corto. */
    private static int raiseTo(PokerState st, int pot, double strength, PokerState.Seat me,
                               int minRaiseTo, int maxRaiseTo, Random rnd) {
        // Stack corto rispetto al piatto: rilanciare "a metà" regala solo informazione, meglio all-in.
        if (strength > 0.85 && me.chips() <= pot * 2) return maxRaiseTo;
        double fraction = 0.45 + rnd.nextDouble() * 0.4;
        int target = st.currentBet() + (int) Math.round(Math.max(pot, st.bigBlind()) * fraction);
        return Math.max(minRaiseTo, Math.min(target, maxRaiseTo));
    }

    /** Stima 0..1 della forza della mano: Chen preflop, categoria della migliore mano dopo. */
    private static double strength(PokerState st, PokerState.Seat me) {
        int[] hole = me.hole();
        if (hole.length < 2) return 0;
        int[] board = st.board();
        if (board.length == 0) return clamp(chen(hole) / 20.0);

        int[] all = new int[hole.length + board.length];
        System.arraycopy(hole, 0, all, 0, hole.length);
        System.arraycopy(board, 0, all, hole.length, board.length);
        int score = PokerHands.best(all);
        int category = PokerHands.category(score);
        double base = switch (category) {
            case PokerHands.PAIR -> 0.38;
            case PokerHands.TWO_PAIR -> 0.60;
            case PokerHands.TRIPS -> 0.74;
            case PokerHands.STRAIGHT -> 0.84;
            case PokerHands.FLUSH -> 0.89;
            case PokerHands.FULL_HOUSE -> 0.95;
            case PokerHands.QUADS, PokerHands.STRAIGHT_FLUSH -> 0.99;
            default -> 0.14;
        };

        // Mano che vive solo sul board (le proprie carte non entrano nelle migliori cinque):
        // vale poco, perché è la stessa mano che hanno tutti gli altri.
        if (category <= PokerHands.PAIR) {
            int[] best = PokerHands.bestFive(all);
            boolean mine = false;
            for (int c : best) {
                if (c == hole[0] || c == hole[1]) { mine = true; break; }
            }
            if (!mine) return 0.08;
            // Carta alta: conta quanto è alta, non è tutta uguale.
            if (category == PokerHands.HIGH_CARD) {
                int top = Math.max(PokerHands.rank(hole[0]), PokerHands.rank(hole[1]));
                return clamp(0.08 + top * 0.012);
            }
        }
        return base;
    }

    /**
     * Punteggio di Chen della mano coperta (scala ~ -1..20): valore della carta più alta,
     * bonus per coppia e per semi uguali, penalità per la distanza tra i ranghi.
     */
    static double chen(int[] hole) {
        int r1 = PokerHands.rank(hole[0]);
        int r2 = PokerHands.rank(hole[1]);
        int high = Math.max(r1, r2);
        int low = Math.min(r1, r2);
        double score = switch (high) {
            case 12 -> 10;  // Asso
            case 11 -> 8;   // Re
            case 10 -> 7;   // Donna
            case 9 -> 6;    // Jack
            default -> (high + 2) / 2.0;
        };
        if (r1 == r2) score = Math.max(5, score * 2);
        if (PokerHands.suit(hole[0]) == PokerHands.suit(hole[1])) score += 2;
        int gap = high - low;
        if (gap == 2) score -= 1;
        else if (gap == 3) score -= 2;
        else if (gap == 4) score -= 4;
        else if (gap > 4) score -= 5;
        if (gap <= 2 && r1 != r2 && high < 10) score += 1; // connettori bassi: potenziale di scala
        return score;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
