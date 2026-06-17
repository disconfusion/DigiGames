package it.digitaliasistemi.minigames.game.minesweeper;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stato di una partita di Campo Minato co-op (board condivisa).
 * Logica pura, nessuna dipendenza da framework.
 * Thread-safe: tutti i metodi pubblici che modificano stato sono synchronized.
 *
 * Costanti facili da cambiare:
 */
public class MinesweeperState {

    public static final int ROWS  = 9;
    public static final int COLS  = 9;
    public static final int MINES = 10;

    public enum Status { PLAYING, WON, LOST }

    // ---- stato della board ----
    private final boolean[][] mine;      // posizione mine (segreta finché PLAYING)
    private final boolean[][] revealed;
    private final boolean[][] flagged;
    private final int[][]     adjacent;  // numero mine adiacenti (0-8)

    private Status status = Status.PLAYING;
    private int flagsUsed = 0;
    private int revealedCount = 0;       // celle non-mina rivelate
    private final int safeCells;         // totale celle non-mina

    // ----------------------------------------------------------------
    // Costruttori
    // ----------------------------------------------------------------

    /** Costruttore per uso produzione: piazza MINES mine con SecureRandom. */
    public MinesweeperState() {
        this(generateRandomMines(ROWS, COLS, MINES));
    }

    /**
     * Costruttore deterministico per i test: accetta la matrice di mine.
     * mine[r][c] == true → cella (r,c) contiene una mina.
     */
    public MinesweeperState(boolean[][] mines) {
        this.mine     = deepCopy(mines);
        this.revealed = new boolean[ROWS][COLS];
        this.flagged  = new boolean[ROWS][COLS];
        this.adjacent = new int[ROWS][COLS];

        // pre-calcola mine adiacenti
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                adjacent[r][c] = countAdjacent(r, c);
            }
        }

        // celle non-mina
        int safe = 0;
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                if (!mine[r][c]) safe++;
        this.safeCells = safe;
    }

    // ----------------------------------------------------------------
    // Azioni pubbliche
    // ----------------------------------------------------------------

    /**
     * Rivela la cella (r,c).
     * Ritorna false se l'azione è ignorata (già rivelata, flagged, o partita non PLAYING).
     */
    public synchronized boolean reveal(int r, int c) {
        if (status != Status.PLAYING) return false;
        if (!inBounds(r, c)) return false;
        if (revealed[r][c] || flagged[r][c]) return false;

        if (mine[r][c]) {
            // colpita una mina → LOST; rivela tutte le mine
            revealed[r][c] = true;
            status = Status.LOST;
            return true;
        }

        // flood-fill (iterativo per evitare stack overflow su board grandi)
        floodReveal(r, c);

        if (revealedCount >= safeCells) {
            status = Status.WON;
        }
        return true;
    }

    /**
     * Toggle bandierina sulla cella (r,c).
     * Ignorato se la cella è già rivelata o la partita non è PLAYING.
     * Ritorna false se ignorato.
     */
    public synchronized boolean flag(int r, int c) {
        if (status != Status.PLAYING) return false;
        if (!inBounds(r, c)) return false;
        if (revealed[r][c]) return false;

        if (flagged[r][c]) {
            flagged[r][c] = false;
            flagsUsed--;
        } else {
            flagged[r][c] = true;
            flagsUsed++;
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public synchronized Status status() { return status; }
    public synchronized int flagsUsed() { return flagsUsed; }

    /** Snapshot delle celle per il protocollo di rete. */
    public synchronized CellSnapshot[][] cellSnapshot() {
        boolean revealMines = (status != Status.PLAYING);
        CellSnapshot[][] snap = new CellSnapshot[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                boolean rev  = revealed[r][c];
                boolean flag = flagged[r][c];
                Integer adj  = rev ? adjacent[r][c] : null;
                // mine: incluso SOLO se rivelata OPPURE partita terminata
                Boolean isMine = (rev || revealMines) ? mine[r][c] : null;
                snap[r][c] = new CellSnapshot(rev, flag, adj, isMine);
            }
        }
        return snap;
    }

    // ----------------------------------------------------------------
    // Logica interna
    // ----------------------------------------------------------------

    private void floodReveal(int startR, int startC) {
        // BFS iterativo
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        queue.add(new int[]{startR, startC});

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int r = pos[0], c = pos[1];
            if (!inBounds(r, c) || revealed[r][c] || flagged[r][c] || mine[r][c]) continue;

            revealed[r][c] = true;
            revealedCount++;

            // se zero mine adiacenti, propaga verso tutti gli 8 vicini
            if (adjacent[r][c] == 0) {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = r + dr, nc = c + dc;
                        if (inBounds(nr, nc) && !revealed[nr][nc]) {
                            queue.add(new int[]{nr, nc});
                        }
                    }
                }
            }
        }
    }

    private int countAdjacent(int r, int c) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (inBounds(nr, nc) && mine[nr][nc]) count++;
            }
        }
        return count;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < ROWS && c >= 0 && c < COLS;
    }

    // ----------------------------------------------------------------
    // Utilità statiche
    // ----------------------------------------------------------------

    private static boolean[][] generateRandomMines(int rows, int cols, int mineCount) {
        List<Integer> positions = new ArrayList<>(rows * cols);
        for (int i = 0; i < rows * cols; i++) positions.add(i);
        Collections.shuffle(positions, new SecureRandom());

        boolean[][] mines = new boolean[rows][cols];
        for (int i = 0; i < mineCount; i++) {
            int idx = positions.get(i);
            mines[idx / cols][idx % cols] = true;
        }
        return mines;
    }

    private static boolean[][] deepCopy(boolean[][] src) {
        boolean[][] copy = new boolean[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = src[i].clone();
        }
        return copy;
    }

    // ----------------------------------------------------------------
    // Record snapshot (serializzato da Jackson via getters/fields)
    // ----------------------------------------------------------------

    public record CellSnapshot(boolean revealed, boolean flagged, Integer adjacent, Boolean mine) {}
}
