package it.digitaliasistemi.minigames.game.dama;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stato puro della Dama italiana. 8x8, 12 pedine a testa sulle case scure.
 * Regole implementate:
 *  - le pedine muovono e catturano solo in avanti (in diagonale);
 *  - la dama muove e cattura di UNA casa in qualsiasi diagonale (non "volante");
 *  - presa obbligatoria: se esiste una cattura, si deve catturare;
 *  - catture multiple concatenate con lo stesso pezzo;
 *  - la pedina NON può catturare una dama;
 *  - promozione a dama sull'ultima traversa; la promozione termina il turno.
 * Semplificazione: non si impone la regola del "numero massimo" di prese.
 * "W" parte in basso e muove verso l'alto (riga decrescente); "B" il contrario.
 */
public class DamaState {

    public enum Status { PLAYING, WON }

    public static final int SIZE = 8;

    public static final class Piece {
        public final String color; // "W" | "B"
        public boolean king;
        Piece(String color, boolean king) { this.color = color; this.king = king; }
    }

    private final Piece[][] board = new Piece[SIZE][SIZE];
    private final Map<String, String> seats = new LinkedHashMap<>(); // email -> "W"|"B"
    private final String whitePlayer;
    private final String blackPlayer;
    private String turn = "W";
    private Status status = Status.PLAYING;
    private String winner;
    /** Se != null, il giocatore deve continuare la cattura con il pezzo in questa posizione. */
    private int[] mustContinue;

    public DamaState(String whitePlayer, String blackPlayer) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        seats.put(whitePlayer, "W");
        seats.put(blackPlayer, "B");
        // Nero in alto (righe 0-2), Bianco in basso (righe 5-7), sulle case scure (r+c dispari).
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if ((r + c) % 2 == 1) {
                    if (r <= 2) board[r][c] = new Piece("B", false);
                    else if (r >= 5) board[r][c] = new Piece("W", false);
                }
            }
        }
    }

    public synchronized String currentTurn() {
        return "W".equals(turn) ? whitePlayer : blackPlayer;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < SIZE && c >= 0 && c < SIZE;
    }

    private String opponent(String color) {
        return "W".equals(color) ? "B" : "W";
    }

    /** Direzioni di avanzamento (dr) per le pedine. */
    private int forward(String color) {
        return "W".equals(color) ? -1 : 1;
    }

    /**
     * Esegue una mossa dal (fr,fc) al (tr,tc).
     * Ritorna false se illegale (turno, geometria, presa obbligatoria non rispettata, ecc.).
     */
    public synchronized boolean move(String email, int fr, int fc, int tr, int tc) {
        if (status != Status.PLAYING) return false;
        String color = seats.get(email);
        if (color == null || !color.equals(turn)) return false;
        if (!inBounds(fr, fc) || !inBounds(tr, tc)) return false;
        Piece p = board[fr][fc];
        if (p == null || !p.color.equals(color)) return false;
        if (board[tr][tc] != null) return false;
        if (mustContinue != null && (mustContinue[0] != fr || mustContinue[1] != fc)) return false;

        boolean mustCapture = mustContinue != null || hasAnyCapture(color);
        int dr = tr - fr;
        int dc = tc - fc;

        // Tentativo di cattura: salto di 2 in diagonale.
        if (Math.abs(dr) == 2 && Math.abs(dc) == 2) {
            int mr = fr + dr / 2;
            int mc = fc + dc / 2;
            Piece mid = board[mr][mc];
            if (mid == null || !mid.color.equals(opponent(color))) return false;
            if (!p.king && mid.king) return false;            // pedina non mangia dama
            if (!p.king && dr != 2 * forward(color)) return false; // pedina cattura solo avanti
            // esegui cattura
            board[tr][tc] = p;
            board[fr][fc] = null;
            board[mr][mc] = null;
            boolean promoted = maybePromote(p, tr);
            if (!promoted && canCaptureFrom(tr, tc)) {
                mustContinue = new int[]{tr, tc};
                return true; // stesso giocatore continua
            }
            endTurn(email, color);
            return true;
        }

        // Mossa semplice: 1 in diagonale, ma vietata se c'è una presa obbligatoria.
        if (Math.abs(dr) == 1 && Math.abs(dc) == 1) {
            if (mustCapture) return false;
            if (!p.king && dr != forward(color)) return false; // pedina solo avanti
            board[tr][tc] = p;
            board[fr][fc] = null;
            maybePromote(p, tr);
            endTurn(email, color);
            return true;
        }

        return false;
    }

    private boolean maybePromote(Piece p, int tr) {
        if (!p.king) {
            if (("W".equals(p.color) && tr == 0) || ("B".equals(p.color) && tr == SIZE - 1)) {
                p.king = true;
                return true;
            }
        }
        return false;
    }

    private void endTurn(String mover, String color) {
        mustContinue = null;
        turn = opponent(color);
        // Se l'avversario non ha mosse legali (o pezzi), il giocatore che ha mosso vince.
        if (!hasAnyMove(turn)) {
            status = Status.WON;
            winner = mover;
        }
    }

    /** Esiste almeno una cattura disponibile per il colore? */
    private boolean hasAnyCapture(String color) {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++) {
                Piece p = board[r][c];
                if (p != null && p.color.equals(color) && canCaptureFrom(r, c)) return true;
            }
        return false;
    }

    /** Il pezzo in (r,c) può catturare? */
    private boolean canCaptureFrom(int r, int c) {
        Piece p = board[r][c];
        if (p == null) return false;
        int[] drs = p.king ? new int[]{-1, 1} : new int[]{forward(p.color)};
        for (int dr : drs) {
            for (int dc : new int[]{-1, 1}) {
                int mr = r + dr, mc = c + dc;
                int tr = r + 2 * dr, tc = c + 2 * dc;
                if (!inBounds(tr, tc) || board[tr][tc] != null) continue;
                Piece mid = board[mr][mc];
                if (mid == null || !mid.color.equals(opponent(p.color))) continue;
                if (!p.king && mid.king) continue; // pedina non mangia dama
                return true;
            }
        }
        return false;
    }

    /** Il colore ha almeno una mossa legale (cattura o semplice)? */
    private boolean hasAnyMove(String color) {
        if (hasAnyCapture(color)) return true;
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++) {
                Piece p = board[r][c];
                if (p == null || !p.color.equals(color)) continue;
                int[] drs = p.king ? new int[]{-1, 1} : new int[]{forward(color)};
                for (int dr : drs)
                    for (int dc : new int[]{-1, 1}) {
                        int tr = r + dr, tc = c + dc;
                        if (inBounds(tr, tc) && board[tr][tc] == null) return true;
                    }
            }
        return false;
    }

    // --- Lettura stato ------------------------------------------------------

    public synchronized Status status() { return status; }
    public synchronized String winner() { return winner; }
    public synchronized Map<String, String> seats() { return new LinkedHashMap<>(seats); }
    public synchronized int[] mustContinue() { return mustContinue == null ? null : mustContinue.clone(); }

    /** Board serializzabile: matrice 8x8 di null o {color, king}. */
    public synchronized List<List<Map<String, Object>>> board() {
        List<List<Map<String, Object>>> grid = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            List<Map<String, Object>> row = new ArrayList<>();
            for (int c = 0; c < SIZE; c++) {
                Piece p = board[r][c];
                if (p == null) {
                    row.add(null);
                } else {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("color", p.color);
                    m.put("king", p.king);
                    row.add(m);
                }
            }
            grid.add(row);
        }
        return grid;
    }
}
