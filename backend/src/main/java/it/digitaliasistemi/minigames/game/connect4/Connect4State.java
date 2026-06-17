package it.digitaliasistemi.minigames.game.connect4;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stato di una partita di Forza 4. Logica pura, nessuna dipendenza da framework.
 * Board 6 righe x 7 colonne. Riga 0 = in alto, riga 5 = in basso (gravità verso il basso).
 */
public class Connect4State {

    public enum Status { PLAYING, WON, DRAW }

    public static final int ROWS = 6;
    public static final int COLS = 7;

    /** board[row][col] — null = vuota, "R" = rosso, "Y" = giallo. */
    private final String[][] board = new String[ROWS][COLS];

    /** Mappa email -> colore assegnato ("R" o "Y"). */
    private final Map<String, String> seats;

    /** Email del giocatore di cui è il turno. null a partita terminata. */
    private String currentTurn;

    private Status status = Status.PLAYING;
    private String winner = null;

    /**
     * Crea una nuova partita con due giocatori.
     *
     * @param player1 email del giocatore 1 — colore "R", inizia per primo
     * @param player2 email del giocatore 2 — colore "Y"
     */
    public Connect4State(String player1, String player2) {
        seats = new LinkedHashMap<>();
        seats.put(player1, "R");
        seats.put(player2, "Y");
        currentTurn = player1;
    }

    /**
     * Costruttore per test: permette di iniettare una board pre-costruita.
     * La board viene valutata immediatamente per determinare lo stato (DRAW / WON).
     * Visibilità package-private: usato solo da Connect4StateTest.
     *
     * @param player1    email del giocatore 1 (R)
     * @param player2    email del giocatore 2 (Y)
     * @param boardState matrice 6x7 già popolata (null | "R" | "Y")
     */
    Connect4State(String player1, String player2, String[][] boardState) {
        seats = new LinkedHashMap<>();
        seats.put(player1, "R");
        seats.put(player2, "Y");
        for (int r = 0; r < ROWS; r++) {
            System.arraycopy(boardState[r], 0, board[r], 0, COLS);
        }
        // Determina lo stato: prima controlla vittorie, poi pareggio, altrimenti PLAYING.
        String winner = detectWinner();
        if (winner != null) {
            this.status = Status.WON;
            this.winner = playerByColor(winner);
            this.currentTurn = null;
        } else if (isBoardFull()) {
            this.status = Status.DRAW;
            this.currentTurn = null;
        } else {
            // Partita in corso: turno a player1 per default
            this.currentTurn = player1;
        }
    }

    /**
     * Tenta di inserire un disco nella colonna indicata per il giocatore dato.
     *
     * @param playerEmail email del giocatore che effettua la mossa
     * @param col         colonna (0..6)
     * @return true se la mossa è accettata, false se illegale
     */
    public synchronized boolean drop(String playerEmail, int col) {
        if (status != Status.PLAYING) return false;
        if (!playerEmail.equals(currentTurn)) return false;
        if (col < 0 || col >= COLS) return false;

        // Trova la riga più bassa libera (gravità verso il basso)
        int row = -1;
        for (int r = ROWS - 1; r >= 0; r--) {
            if (board[r][col] == null) {
                row = r;
                break;
            }
        }
        if (row == -1) return false; // colonna piena

        String color = seats.get(playerEmail);
        board[row][col] = color;

        // Controlla vittoria
        if (checkWin(row, col, color)) {
            status = Status.WON;
            winner = playerEmail;
            currentTurn = null;
            return true;
        }

        // Controlla pareggio (board piena)
        if (isBoardFull()) {
            status = Status.DRAW;
            currentTurn = null;
            return true;
        }

        // Passa il turno all'altro giocatore
        for (Map.Entry<String, String> e : seats.entrySet()) {
            if (!e.getKey().equals(playerEmail)) {
                currentTurn = e.getKey();
                break;
            }
        }
        return true;
    }

    // ---- Logica di vittoria ----

    private boolean checkWin(int row, int col, String color) {
        return countDir(row, col, color, 0, 1) >= 4    // orizzontale
            || countDir(row, col, color, 1, 0) >= 4    // verticale
            || countDir(row, col, color, 1, 1) >= 4    // diagonale \
            || countDir(row, col, color, 1, -1) >= 4;  // diagonale /
    }

    /**
     * Conta i dischi consecutivi dello stesso colore lungo una direzione (entrambi i versi).
     * dr = delta riga, dc = delta colonna.
     */
    private int countDir(int row, int col, String color, int dr, int dc) {
        int count = 1; // include la cella appena piazzata
        for (int sign : new int[]{1, -1}) {
            for (int i = 1; i < 4; i++) {
                int r = row + sign * dr * i;
                int c = col + sign * dc * i;
                if (r < 0 || r >= ROWS || c < 0 || c >= COLS || !color.equals(board[r][c])) break;
                count++;
            }
        }
        return count;
    }

    /** Usato dal costruttore di test: scansiona l'intera board per trovare un vincitore. */
    private String detectWinner() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                String color = board[r][c];
                if (color == null) continue;
                if (checkWin(r, c, color)) return color;
            }
        }
        return null;
    }

    /** Restituisce l'email del giocatore con il colore dato. */
    private String playerByColor(String color) {
        for (Map.Entry<String, String> e : seats.entrySet()) {
            if (e.getValue().equals(color)) return e.getKey();
        }
        return null;
    }

    private boolean isBoardFull() {
        for (int c = 0; c < COLS; c++) {
            if (board[0][c] == null) return false;
        }
        return true;
    }

    // ---- Accessori ----

    /** Restituisce una copia della board come matrice di stringhe (null | "R" | "Y"). */
    public synchronized String[][] board() {
        String[][] copy = new String[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            System.arraycopy(board[r], 0, copy[r], 0, COLS);
        }
        return copy;
    }

    public synchronized Status status() { return status; }
    public synchronized String winner() { return winner; }
    public synchronized String currentTurn() { return currentTurn; }

    /** Mappa email -> colore (snapshot immutabile). */
    public Map<String, String> seats() { return Map.copyOf(seats); }
}
