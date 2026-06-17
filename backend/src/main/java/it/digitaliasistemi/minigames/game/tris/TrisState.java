package it.digitaliasistemi.minigames.game.tris;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stato puro del Tris (tris/filetto): 2 giocatori, griglia 3x3.
 * "X" muove per primo. Server autorità: ogni mossa è validata qui.
 */
public class TrisState {

    public enum Status { PLAYING, WON, DRAW }

    public static final int SIZE = 3;

    private static final int[][] LINES = {
        {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // righe
        {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // colonne
        {0, 4, 8}, {2, 4, 6}             // diagonali
    };

    private final String[] cells = new String[9]; // null | "X" | "O"
    private final Map<String, String> seats = new LinkedHashMap<>(); // email -> "X"|"O"
    private final String playerX;
    private final String playerO;
    private String turnMark = "X";
    private Status status = Status.PLAYING;
    private String winner; // email vincitore, null se nessuno

    public TrisState(String playerX, String playerO) {
        this.playerX = playerX;
        this.playerO = playerO;
        seats.put(playerX, "X");
        seats.put(playerO, "O");
    }

    /** Email del giocatore di turno. */
    public synchronized String currentTurn() {
        return "X".equals(turnMark) ? playerX : playerO;
    }

    /**
     * Posiziona il segno del giocatore nella cella pos (0..8).
     * Ritorna false se non è il suo turno, cella occupata/fuori range, o partita finita.
     */
    public synchronized boolean place(String email, int pos) {
        if (status != Status.PLAYING) return false;
        if (pos < 0 || pos >= cells.length) return false;
        if (cells[pos] != null) return false;
        String mark = seats.get(email);
        if (mark == null || !mark.equals(turnMark)) return false;

        cells[pos] = mark;

        if (isWin(mark)) {
            status = Status.WON;
            winner = email;
        } else if (isFull()) {
            status = Status.DRAW;
        } else {
            turnMark = "X".equals(turnMark) ? "O" : "X";
        }
        return true;
    }

    private boolean isWin(String mark) {
        for (int[] line : LINES) {
            if (mark.equals(cells[line[0]]) && mark.equals(cells[line[1]]) && mark.equals(cells[line[2]])) {
                return true;
            }
        }
        return false;
    }

    private boolean isFull() {
        for (String c : cells) if (c == null) return false;
        return true;
    }

    /** Board come matrice 3x3 (null|"X"|"O") per il frontend. */
    public synchronized String[][] board() {
        String[][] grid = new String[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                grid[r][c] = cells[r * SIZE + c];
            }
        }
        return grid;
    }

    public synchronized Status status() { return status; }
    public synchronized String winner() { return winner; }
    public synchronized Map<String, String> seats() { return new LinkedHashMap<>(seats); }
}
