package it.digitaliasistemi.minigames.game.battlecity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mappe di Battle City. Ogni mappa è una griglia {@value #TILES}×{@value #TILES} di tile da
 * {@value BattleCityState#TILE} px, scritta come stringhe (come le sprite pixel del frontend):
 *
 * <pre>
 *   . vuoto      B mattoni (distruttibili)   S acciaio (serve il cannone potenziato)
 *   W acqua      T cespugli (coprono)        I ghiaccio (si slitta)
 * </pre>
 *
 * <p>La base (aquila) e i mattoni che la proteggono vengono aggiunti a parte da
 * {@link BattleCityState}: così nessuna mappa può dimenticarli o murarli.
 *
 * <p>I livelli oltre il sesto sono generati: simmetrici rispetto all'asse verticale (come
 * l'originale), sempre con corridoi liberi verso la base e attorno agli spawn.
 */
public final class BattleCityMaps {

    private BattleCityMaps() {}

    /** Lato del campo in tile. */
    public static final int TILES = 13;

    private static final List<String[]> HAND_DRAWN = List.of(
        // 1 — Colonne di mattoni: la mappa di apertura dell'originale
        new String[] {
            ".............",
            "..BB.BB.BB...",
            "..BB.BB.BB...",
            "..BB.BB.BB...",
            "..BB.BB.BB...",
            "..BB.BB.BB...",
            "SS.BB.BB.BB.S",
            "..BB.BB.BB...",
            "..BB.BB.BB...",
            "..BB.BB.BB...",
            "..BB.BB.BB...",
            ".............",
            "............."
        },
        // 2 — Fortini d'acciaio e canali d'acqua
        new String[] {
            ".............",
            ".BBB.SSS.BBB.",
            ".B.....W...B.",
            ".B.WW..W..BB.",
            "...WW..W.....",
            "BB....SS...BB",
            "BB.SS....S.BB",
            "......WW.....",
            ".BB..WW..BB..",
            ".BB......BB..",
            "...SSS.SSS...",
            ".............",
            "............."
        },
        // 3 — Labirinto di mattoni con cespugli per gli agguati
        new String[] {
            ".............",
            "BBBB.TTT.BBBB",
            "B..B.....B..B",
            "B.BB.BBB.BB.B",
            "....B...B....",
            "TT..B.S.B..TT",
            "....BB.BB....",
            "..BB.....BB..",
            "..B.SSSSS.B..",
            "..B.......B..",
            "..BBB.BBBB...",
            ".............",
            "............."
        },
        // 4 — Ghiaccio: si slitta, mirare diventa difficile
        new String[] {
            "III.......III",
            "III.BBBBB.III",
            "....B...B....",
            ".BB.B.S.B.BB.",
            ".BB.B...B.BB.",
            "....BB.BB....",
            "SS....I....SS",
            "....BB.BB....",
            ".BB.B...B.BB.",
            ".BB.B.S.B.BB.",
            "III.BBBBB.III",
            "III.......III",
            "............."
        },
        // 5 — Corridoi stretti e acqua a fasce
        new String[] {
            ".............",
            ".B.B.B.B.B.B.",
            ".B.B.B.B.B.B.",
            ".B.B.B.B.B.B.",
            "WWWWW.WWWWWWW",
            ".............",
            "S.SS.....SS.S",
            ".............",
            "WWWWWWW.WWWWW",
            ".B.B.B.B.B.B.",
            ".B.B.B.B.B.B.",
            ".B.B.B.B.B.B.",
            "............."
        },
        // 6 — La fortezza: tanto acciaio, passaggi obbligati
        new String[] {
            "..S.......S..",
            "..S.BBBBB.S..",
            "..S.B...B.S..",
            "....B.T.B....",
            "BBB.B...B.BBB",
            "..S.BB.BB.S..",
            "..S.......S..",
            "..S.BB.BB.S..",
            "BBB.B...B.BBB",
            "....B.T.B....",
            "..S.B...B.S..",
            "..S.BBBBB.S..",
            "..S.......S.."
        }
    );

    /** Numero di mappe disegnate a mano (oltre si genera). */
    public static int handDrawnCount() {
        return HAND_DRAWN.size();
    }

    /**
     * Mappa del livello richiesto (1-based). Fino a {@link #handDrawnCount()} sono quelle
     * disegnate; oltre vengono generate in modo deterministico dal numero di livello, così
     * lo stesso livello ha sempre la stessa mappa.
     */
    public static String[] level(int level) {
        int n = Math.max(1, level);
        if (n <= HAND_DRAWN.size()) return HAND_DRAWN.get(n - 1).clone();
        return generate(new Random(n * 7919L), n);
    }

    /**
     * Mappa simmetrica generata: blocchi casuali specchiati sull'asse verticale, con le righe
     * degli spawn e la corsia della base tenute libere. Più alto il livello, più acciaio.
     */
    static String[] generate(Random rnd, int level) {
        char[][] g = new char[TILES][TILES];
        for (char[] row : g) java.util.Arrays.fill(row, '.');

        // Probabilità di acciaio crescente col livello (dal 10% al 40%)
        double steelChance = Math.min(0.4, 0.10 + (level - BattleCityMaps.handDrawnCount()) * 0.03);
        int half = (TILES + 1) / 2;

        for (int r = 1; r < TILES - 1; r++) {
            for (int c = 0; c < half; c++) {
                // Righe di spawn (0-1) e corsia della base (ultime due) restano libere
                if (r <= 1 || r >= TILES - 2) continue;
                double p = rnd.nextDouble();
                char t = '.';
                if (p < 0.34) t = 'B';
                else if (p < 0.34 + steelChance * 0.5) t = 'S';
                else if (p < 0.50) t = 'W';
                else if (p < 0.56) t = 'T';
                else if (p < 0.60) t = 'I';
                g[r][c] = t;
                g[r][TILES - 1 - c] = t; // specchia
            }
        }

        // Corridoio verticale centrale sempre percorribile: la base deve essere raggiungibile
        int mid = TILES / 2;
        for (int r = 0; r < TILES; r++) {
            if (g[r][mid] == 'S' || g[r][mid] == 'W') g[r][mid] = '.';
        }
        // Corridoio orizzontale a metà campo: evita mappe spezzate in due
        for (int c = 0; c < TILES; c++) {
            if (g[mid][c] == 'S' || g[mid][c] == 'W') g[mid][c] = '.';
        }
        // Punti di spawn (angoli alti e centro alto) e zona base liberi
        clear(g, 0, 0); clear(g, 0, mid); clear(g, 0, TILES - 1);
        clear(g, 1, 0); clear(g, 1, mid); clear(g, 1, TILES - 1);

        List<String> out = new ArrayList<>(TILES);
        for (char[] row : g) out.add(new String(row));
        return out.toArray(new String[0]);
    }

    private static void clear(char[][] g, int r, int c) {
        if (r >= 0 && r < TILES && c >= 0 && c < TILES) g[r][c] = '.';
    }
}
