package it.digitaliasistemi.minigames.game.minesweeper;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MinesweeperState {

    public static final int ROWS  = 9;
    public static final int COLS  = 9;
    public static final int MINES = 10;

    public enum Status { PLAYING, WON, LOST }

    private final boolean[][] mine;
    private final boolean[][] revealed;
    private final boolean[][] flagged;
    private final int[][]     adjacent;

    private Status status = Status.PLAYING;
    private int flagsUsed = 0;
    private int revealedCount = 0;
    private final int safeCells;

    // Turn tracking — empty list = nessun controllo turno (usato nei test)
    private final List<String> players;
    private int turnIndex = 0;

    // ----------------------------------------------------------------
    // Costruttori
    // ----------------------------------------------------------------

    /** Produzione: mine random, nessun turno. */
    public MinesweeperState() {
        this(generateRandomMines(ROWS, COLS, MINES), List.of());
    }

    /** Test: mine deterministiche, nessun turno. */
    public MinesweeperState(boolean[][] mines) {
        this(mines, List.of());
    }

    /** Produzione con turni: mine random + lista giocatori ordinata. */
    public MinesweeperState(List<String> players) {
        this(generateRandomMines(ROWS, COLS, MINES), players);
    }

    private MinesweeperState(boolean[][] mines, List<String> players) {
        this.mine     = deepCopy(mines);
        this.revealed = new boolean[ROWS][COLS];
        this.flagged  = new boolean[ROWS][COLS];
        this.adjacent = new int[ROWS][COLS];
        this.players  = List.copyOf(players);

        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                adjacent[r][c] = countAdjacent(r, c);

        int safe = 0;
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                if (!mine[r][c]) safe++;
        this.safeCells = safe;
    }

    // ----------------------------------------------------------------
    // Turn helpers
    // ----------------------------------------------------------------

    public synchronized String currentTurn() {
        if (players.isEmpty()) return null;
        return players.get(turnIndex % players.size());
    }

    public synchronized boolean isMyTurn(String player) {
        return players.isEmpty() || player.equals(currentTurn());
    }

    // ----------------------------------------------------------------
    // Azioni pubbliche
    // ----------------------------------------------------------------

    public synchronized boolean reveal(int r, int c) {
        if (status != Status.PLAYING) return false;
        if (!inBounds(r, c)) return false;
        if (revealed[r][c] || flagged[r][c]) return false;

        if (mine[r][c]) {
            revealed[r][c] = true;
            status = Status.LOST;
            return true; // game over: turno non avanza
        }

        floodReveal(r, c);

        if (revealedCount >= safeCells) {
            status = Status.WON;
            return true; // game over: turno non avanza
        }

        if (!players.isEmpty()) turnIndex++;
        return true;
    }

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
        if (!players.isEmpty()) turnIndex++;
        return true;
    }

    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public synchronized Status status() { return status; }
    public synchronized int flagsUsed() { return flagsUsed; }

    public synchronized CellSnapshot[][] cellSnapshot() {
        boolean revealMines = (status != Status.PLAYING);
        CellSnapshot[][] snap = new CellSnapshot[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                boolean rev  = revealed[r][c];
                boolean flag = flagged[r][c];
                Integer adj  = rev ? adjacent[r][c] : null;
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
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        queue.add(new int[]{startR, startC});

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int r = pos[0], c = pos[1];
            if (!inBounds(r, c) || revealed[r][c] || flagged[r][c] || mine[r][c]) continue;

            revealed[r][c] = true;
            revealedCount++;

            if (adjacent[r][c] == 0) {
                for (int dr = -1; dr <= 1; dr++)
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = r + dr, nc = c + dc;
                        if (inBounds(nr, nc) && !revealed[nr][nc]) queue.add(new int[]{nr, nc});
                    }
            }
        }
    }

    private int countAdjacent(int r, int c) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (inBounds(nr, nc) && mine[nr][nc]) count++;
            }
        return count;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < ROWS && c >= 0 && c < COLS;
    }

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
        for (int i = 0; i < src.length; i++) copy[i] = src[i].clone();
        return copy;
    }

    public record CellSnapshot(boolean revealed, boolean flagged, Integer adjacent, Boolean mine) {}
}
