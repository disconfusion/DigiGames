package it.digitaliasistemi.minigames.game.poker;

import java.util.Arrays;

/**
 * Valutazione delle mani di poker: pura, senza stato, senza framework.
 *
 * <p>Una carta è un {@code int} 0..51 con <b>rango = c / 4</b> (0 = Due … 12 = Asso) e
 * <b>seme = c % 4</b>. Il punteggio di una mano è un {@code int} confrontabile con {@code <}:
 * categoria nei bit alti, poi cinque ranghi in nibble in ordine di significatività
 * ({@code cat<<20 | r1<<16 | r2<<12 | r3<<8 | r4<<4 | r5}). I nibble non significativi
 * valgono 0: il confronto avviene solo tra mani della stessa categoria, dove le posizioni
 * hanno lo stesso significato, quindi il padding non falsa mai l'ordine.
 */
public final class PokerHands {

    public static final int SUITS = 4;
    public static final int RANKS = 13;
    public static final int DECK = SUITS * RANKS;

    public static final int HIGH_CARD = 0;
    public static final int PAIR = 1;
    public static final int TWO_PAIR = 2;
    public static final int TRIPS = 3;
    public static final int STRAIGHT = 4;
    public static final int FLUSH = 5;
    public static final int FULL_HOUSE = 6;
    public static final int QUADS = 7;
    public static final int STRAIGHT_FLUSH = 8;

    /** Etichette dei ranghi, indicizzate dal rango della carta. */
    private static final String[] RANK_LABEL = {
        "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"
    };
    /** Nomi dei semi, indicizzati dal seme della carta. */
    private static final String[] SUIT_NAME = { "spade", "cuori", "quadri", "fiori" };

    private static final String[] CATEGORY_NAME = {
        "Carta alta", "Coppia", "Doppia coppia", "Tris", "Scala", "Colore",
        "Full", "Poker", "Scala colore"
    };

    private PokerHands() {}

    public static int rank(int card) { return card / SUITS; }

    public static int suit(int card) { return card % SUITS; }

    public static String rankLabel(int card) { return RANK_LABEL[rank(card)]; }

    public static String suitName(int card) { return SUIT_NAME[suit(card)]; }

    public static String categoryName(int score) { return CATEGORY_NAME[category(score)]; }

    /** Categoria estratta da un punteggio (una delle costanti {@code HIGH_CARD}…{@code STRAIGHT_FLUSH}). */
    public static int category(int score) { return score >>> 20; }

    /**
     * Punteggio della migliore mano di 5 carte ricavabile dalle carte date (ne bastano 5, al
     * massimo 7: le due coperte più il board completo).
     */
    public static int best(int[] cards) {
        return bestCombo(cards)[0];
    }

    /** Le 5 carte che formano la migliore mano (utile per evidenziarle allo showdown). */
    public static int[] bestFive(int[] cards) {
        int[] combo = bestCombo(cards);
        return Arrays.copyOfRange(combo, 1, 6);
    }

    /** {@code [score, c1..c5]} della migliore combinazione: un solo passaggio sulle 21 combinazioni. */
    private static int[] bestCombo(int[] cards) {
        if (cards.length < 5) throw new IllegalArgumentException("Servono almeno 5 carte");
        int[] five = new int[5];
        int[] bestFive = new int[5];
        int bestScore = -1;
        int n = cards.length;
        for (int a = 0; a < n - 4; a++) {
            for (int b = a + 1; b < n - 3; b++) {
                for (int c = b + 1; c < n - 2; c++) {
                    for (int d = c + 1; d < n - 1; d++) {
                        for (int e = d + 1; e < n; e++) {
                            five[0] = cards[a]; five[1] = cards[b]; five[2] = cards[c];
                            five[3] = cards[d]; five[4] = cards[e];
                            int score = score5(five);
                            if (score > bestScore) {
                                bestScore = score;
                                System.arraycopy(five, 0, bestFive, 0, 5);
                            }
                        }
                    }
                }
            }
        }
        return new int[]{ bestScore, bestFive[0], bestFive[1], bestFive[2], bestFive[3], bestFive[4] };
    }

    /** Punteggio di esattamente 5 carte. */
    static int score5(int[] five) {
        int[] count = new int[RANKS];
        boolean flush = true;
        int firstSuit = suit(five[0]);
        for (int card : five) {
            count[rank(card)]++;
            if (suit(card) != firstSuit) flush = false;
        }

        int straightHigh = straightHigh(count);
        if (flush && straightHigh >= 0) return pack(STRAIGHT_FLUSH, straightHigh);

        // Ranghi raggruppati per molteplicità decrescente, poi per rango decrescente:
        // è esattamente l'ordine dei tiebreaker in tutte le categorie "per gruppi".
        int quad = -1, trips = -1, highPair = -1, lowPair = -1;
        for (int r = RANKS - 1; r >= 0; r--) {
            switch (count[r]) {
                case 4 -> quad = r;
                case 3 -> trips = r;
                case 2 -> { if (highPair < 0) highPair = r; else lowPair = r; }
                default -> { }
            }
        }

        if (quad >= 0) return pack(QUADS, concat(new int[]{ quad }, kickers(count, 1, quad)));
        if (trips >= 0 && highPair >= 0) return pack(FULL_HOUSE, trips, highPair);
        if (flush) return pack(FLUSH, kickers(count, 5));
        if (straightHigh >= 0) return pack(STRAIGHT, straightHigh);
        if (trips >= 0) return pack(TRIPS, concat(new int[]{ trips }, kickers(count, 2, trips)));
        if (lowPair >= 0) {
            return pack(TWO_PAIR, concat(new int[]{ highPair, lowPair }, kickers(count, 1, highPair, lowPair)));
        }
        if (highPair >= 0) return pack(PAIR, concat(new int[]{ highPair }, kickers(count, 3, highPair)));
        return pack(HIGH_CARD, kickers(count, 5));
    }

    /**
     * Rango più alto della scala contenuta nei conteggi, -1 se non c'è.
     * L'Asso conta anche come "uno" nella scala minima A-2-3-4-5, dove la carta alta è il Cinque.
     */
    private static int straightHigh(int[] count) {
        for (int high = RANKS - 1; high >= 4; high--) {
            boolean run = true;
            for (int r = high; r > high - 5; r--) {
                if (count[r] == 0) { run = false; break; }
            }
            if (run) return high;
        }
        // Scala minima: 5-4-3-2-A (rango 3 = Cinque come carta alta).
        if (count[12] > 0 && count[0] > 0 && count[1] > 0 && count[2] > 0 && count[3] > 0) return 3;
        return -1;
    }

    /** I {@code howMany} ranghi più alti non ancora usati, dal più alto (ripetizioni incluse). */
    private static int[] kickers(int[] count, int howMany, int... exclude) {
        int[] out = new int[howMany];
        int i = 0;
        for (int r = RANKS - 1; r >= 0 && i < howMany; r--) {
            if (count[r] == 0 || contains(exclude, r)) continue;
            for (int k = 0; k < count[r] && i < howMany; k++) out[i++] = r;
        }
        return out;
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values) if (v == value) return true;
        return false;
    }

    /** Impacchetta categoria e tiebreaker, dal più significativo al meno. */
    private static int pack(int category, int... parts) {
        int score = category << 20;
        int shift = 16;
        for (int p : parts) {
            score |= (p & 0xF) << shift;
            shift -= 4;
        }
        return score;
    }

    private static int[] concat(int[] head, int[] tail) {
        int[] out = Arrays.copyOf(head, head.length + tail.length);
        System.arraycopy(tail, 0, out, head.length, tail.length);
        return out;
    }
}
