package it.digitaliasistemi.minigames.game.chess;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stato puro di una partita di scacchi con regole complete:
 * mosse legali per pezzo, scacco, scacco matto, stallo, arrocco, en passant, promozione.
 * Server autorità: ogni mossa è validata contro l'elenco delle mosse legali.
 *
 * Coordinate: riga 0 = traversa 8 (in alto, pezzi neri), riga 7 = traversa 1 (in basso, bianchi).
 * Il Bianco muove verso righe decrescenti.
 */
public class ChessState {

    public enum Status {
        PLAYING, CHECKMATE, STALEMATE,
        DRAW_INSUFFICIENT, DRAW_REPETITION, DRAW_AGREED,
        RESIGNED, TIMEOUT
    }

    public static final char WHITE = 'W';
    public static final char BLACK = 'B';

    public static final class Piece {
        public final char color; // 'W' | 'B'
        public final char type;  // 'P','N','B','R','Q','K'
        public Piece(char color, char type) { this.color = color; this.type = type; }
    }

    /** Mossa completa, con flag per le mosse speciali. */
    public static final class Move {
        public final int fr, fc, tr, tc;
        public final char promotion; // 0 se nessuna
        public final boolean castle;
        public final boolean enPassant;
        Move(int fr, int fc, int tr, int tc, char promotion, boolean castle, boolean enPassant) {
            this.fr = fr; this.fc = fc; this.tr = tr; this.tc = tc;
            this.promotion = promotion; this.castle = castle; this.enPassant = enPassant;
        }
    }

    private final Piece[][] board = new Piece[8][8];
    private char turn = WHITE;
    private boolean castleWK = true, castleWQ = true, castleBK = true, castleBQ = true;
    private int[] enPassant; // casa bersaglio (r,c) o null
    private Status status = Status.PLAYING;
    private Character winner; // colore vincitore (matto, resa, tempo)

    private final Map<String, Character> seats = new LinkedHashMap<>();
    private final String whitePlayer;
    private final String blackPlayer;

    // Patta per ripetizione: conteggio posizioni
    private final Map<String, Integer> positionCounts = new HashMap<>();
    // Offerta di patta pendente, fatta dal colore indicato (null = nessuna)
    private Character drawOfferBy;

    // Orologio (millisecondi); timed=false = partita senza limite di tempo
    private final boolean timed;
    private long whiteMillis;
    private long blackMillis;
    private Instant turnStartedAt;

    public ChessState(String whitePlayer, String blackPlayer) {
        this(whitePlayer, blackPlayer, 0L);
    }

    /** @param initialMillis tempo a testa in ms; &lt;= 0 = senza limite. */
    public ChessState(String whitePlayer, String blackPlayer, long initialMillis) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        seats.put(whitePlayer, WHITE);
        seats.put(blackPlayer, BLACK);
        setup();
        this.timed = initialMillis > 0;
        if (timed) {
            this.whiteMillis = initialMillis;
            this.blackMillis = initialMillis;
            this.turnStartedAt = Instant.now();
        }
        recordPosition();
    }

    private void setup() {
        char[] back = {'R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R'};
        for (int c = 0; c < 8; c++) {
            board[0][c] = new Piece(BLACK, back[c]);
            board[1][c] = new Piece(BLACK, 'P');
            board[6][c] = new Piece(WHITE, 'P');
            board[7][c] = new Piece(WHITE, back[c]);
        }
    }

    public synchronized String currentTurn() {
        return turn == WHITE ? whitePlayer : blackPlayer;
    }

    private static boolean inB(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    private static char opp(char color) {
        return color == WHITE ? BLACK : WHITE;
    }

    // --- Mossa pubblica -----------------------------------------------------

    /**
     * Esegue la mossa se è tra quelle legali. {@code promotion} è usato solo per la promozione
     * (default 'Q' se omesso). Ritorna false se non valida o non è il turno del giocatore.
     */
    public synchronized boolean move(String email, int fr, int fc, int tr, int tc, char promotion) {
        if (status != Status.PLAYING) return false;
        Character color = seats.get(email);
        if (color == null || color != turn) return false;

        char promo = (promotion == 0) ? 'Q' : Character.toUpperCase(promotion);
        Move chosen = null;
        for (Move m : legalMoves(turn)) {
            if (m.fr == fr && m.fc == fc && m.tr == tr && m.tc == tc) {
                if (m.promotion == 0 || m.promotion == promo) {
                    chosen = m;
                    break;
                }
            }
        }
        if (chosen == null) return false;

        applyReal(chosen);
        return true;
    }

    private void applyReal(Move m) {
        Piece p = board[m.fr][m.fc];

        // Aggiorna diritti di arrocco
        if (p.type == 'K') {
            if (p.color == WHITE) { castleWK = false; castleWQ = false; }
            else { castleBK = false; castleBQ = false; }
        }
        updateRookRights(m.fr, m.fc);
        updateRookRights(m.tr, m.tc); // se cattura una torre nel suo angolo

        int[] newEp = null;
        if (p.type == 'P' && Math.abs(m.tr - m.fr) == 2) {
            newEp = new int[]{(m.fr + m.tr) / 2, m.fc};
        }

        applyToBoard(board, m, p.color);
        enPassant = newEp;
        char mover = turn;
        turn = opp(turn);
        drawOfferBy = null; // ogni mossa annulla un'eventuale offerta di patta

        // Orologio: scala il tempo usato dal giocatore che ha mosso
        if (timed) {
            Instant now = Instant.now();
            long elapsed = Duration.between(turnStartedAt, now).toMillis();
            if (mover == WHITE) whiteMillis = Math.max(0, whiteMillis - elapsed);
            else blackMillis = Math.max(0, blackMillis - elapsed);
            turnStartedAt = now;
        }

        recordPosition();
        recomputeStatus();

        // Bandierina: se chi ha mosso ha esaurito il tempo (e non ha dato matto), perde a tempo
        if (status == Status.PLAYING && timed && clockOf(mover) <= 0) {
            status = Status.TIMEOUT;
            winner = opp(mover);
        }
    }

    private long clockOf(char color) {
        return color == WHITE ? whiteMillis : blackMillis;
    }

    private void updateRookRights(int r, int c) {
        if (r == 7 && c == 0) castleWQ = false;
        if (r == 7 && c == 7) castleWK = false;
        if (r == 0 && c == 0) castleBQ = false;
        if (r == 0 && c == 7) castleBK = false;
    }

    /** Applica la mossa su una board (mutando), gestendo cattura, en passant, arrocco, promozione. */
    private void applyToBoard(Piece[][] b, Move m, char color) {
        Piece p = b[m.fr][m.fc];
        b[m.fr][m.fc] = null;

        if (m.enPassant) {
            // pedone catturato è sulla riga di partenza, colonna di arrivo
            b[m.fr][m.tc] = null;
        }

        if (m.promotion != 0) {
            b[m.tr][m.tc] = new Piece(color, m.promotion);
        } else {
            b[m.tr][m.tc] = p;
        }

        if (m.castle) {
            // sposta la torre
            if (m.tc == 6) { // corto
                b[m.tr][5] = b[m.tr][7];
                b[m.tr][7] = null;
            } else if (m.tc == 2) { // lungo
                b[m.tr][3] = b[m.tr][0];
                b[m.tr][0] = null;
            }
        }
    }

    private void recomputeStatus() {
        boolean any = !legalMoves(turn).isEmpty();
        if (!any) {
            if (isInCheck(board, turn)) {
                status = Status.CHECKMATE;
                winner = opp(turn);
            } else {
                status = Status.STALEMATE;
            }
            return;
        }
        if (insufficientMaterial()) {
            status = Status.DRAW_INSUFFICIENT;
            return;
        }
        if (positionCounts.getOrDefault(positionKey(), 0) >= 3) {
            status = Status.DRAW_REPETITION;
            return;
        }
        status = Status.PLAYING;
    }

    /** Chiave posizione per la regola della ripetizione: pezzi + turno + arrocchi + en passant. */
    private String positionKey() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                sb.append(p == null ? "." : ("" + p.color + p.type));
            }
        sb.append('|').append(turn);
        sb.append('|').append(castleWK ? 'K' : '-').append(castleWQ ? 'Q' : '-')
          .append(castleBK ? 'k' : '-').append(castleBQ ? 'q' : '-');
        sb.append('|').append(enPassant == null ? "-" : (enPassant[0] + "," + enPassant[1]));
        return sb.toString();
    }

    private void recordPosition() {
        String key = positionKey();
        positionCounts.merge(key, 1, Integer::sum);
    }

    /** Materiale insufficiente per dare matto (regola "dead position" semplificata). */
    private boolean insufficientMaterial() {
        List<int[]> minors = new ArrayList<>(); // [squareColor] per alfieri; cavalli marcati a parte
        int knights = 0;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p == null) continue;
                switch (p.type) {
                    case 'P', 'R', 'Q' -> { return false; } // materiale sufficiente
                    case 'N' -> { knights++; minors.add(new int[]{-1}); }
                    case 'B' -> minors.add(new int[]{(r + c) % 2});
                    default -> { } // re
                }
            }
        int total = minors.size();
        if (total <= 1) return true;            // K vs K, K vs K+minore
        if (total == 2 && knights == 0) {       // due alfieri: patta se stesso colore di casa
            return minors.get(0)[0] == minors.get(1)[0];
        }
        return false;
    }

    // --- Azioni extra: resa, patta, tempo ----------------------------------

    /** Il giocatore abbandona: l'avversario vince. */
    public synchronized boolean resign(String email) {
        Character color = seats.get(email);
        if (color == null || status != Status.PLAYING) return false;
        status = Status.RESIGNED;
        winner = opp(color);
        return true;
    }

    /** Offerta di patta da parte del giocatore. */
    public synchronized boolean offerDraw(String email) {
        Character color = seats.get(email);
        if (color == null || status != Status.PLAYING) return false;
        drawOfferBy = color;
        return true;
    }

    /** Risposta a un'offerta di patta (deve rispondere l'avversario). */
    public synchronized boolean respondDraw(String email, boolean accept) {
        Character color = seats.get(email);
        if (color == null || status != Status.PLAYING || drawOfferBy == null) return false;
        if (drawOfferBy == color) return false; // non puoi rispondere alla tua offerta
        if (accept) {
            status = Status.DRAW_AGREED;
        } else {
            drawOfferBy = null;
        }
        return true;
    }

    /** Verifica caduta della bandierina del giocatore di turno. Ritorna true se è scaduto il tempo. */
    public synchronized boolean checkTimeout() {
        if (!timed || status != Status.PLAYING) return false;
        long elapsed = Duration.between(turnStartedAt, Instant.now()).toMillis();
        if (clockOf(turn) - elapsed <= 0) {
            status = Status.TIMEOUT;
            winner = opp(turn);
            if (turn == WHITE) whiteMillis = 0; else blackMillis = 0;
            return true;
        }
        return false;
    }

    // --- Generazione mosse --------------------------------------------------

    /** Mosse legali del colore (pseudo-legali filtrate per non lasciare il re sotto scacco). */
    public synchronized List<Move> legalMoves(char color) {
        List<Move> pseudo = pseudoMoves(color, board, enPassant, true);
        List<Move> legal = new ArrayList<>();
        for (Move m : pseudo) {
            Piece[][] copy = copy(board);
            applyToBoard(copy, m, color);
            if (!isInCheck(copy, color)) legal.add(m);
        }
        return legal;
    }

    private List<Move> pseudoMoves(char color, Piece[][] b, int[] ep, boolean withCastling) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = b[r][c];
                if (p == null || p.color != color) continue;
                switch (p.type) {
                    case 'P' -> pawnMoves(moves, b, ep, r, c, color);
                    case 'N' -> stepMoves(moves, b, r, c, color, KNIGHT);
                    case 'K' -> {
                        stepMoves(moves, b, r, c, color, KING);
                        if (withCastling) castlingMoves(moves, b, r, c, color);
                    }
                    case 'B' -> slideMoves(moves, b, r, c, color, DIAG);
                    case 'R' -> slideMoves(moves, b, r, c, color, ORTHO);
                    case 'Q' -> { slideMoves(moves, b, r, c, color, DIAG); slideMoves(moves, b, r, c, color, ORTHO); }
                    default -> { }
                }
            }
        }
        return moves;
    }

    private static final int[][] KNIGHT = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
    private static final int[][] KING = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
    private static final int[][] DIAG = {{-1,-1},{-1,1},{1,-1},{1,1}};
    private static final int[][] ORTHO = {{-1,0},{1,0},{0,-1},{0,1}};

    private void pawnMoves(List<Move> moves, Piece[][] b, int[] ep, int r, int c, char color) {
        int dir = (color == WHITE) ? -1 : 1;
        int startRow = (color == WHITE) ? 6 : 1;
        int promoRow = (color == WHITE) ? 0 : 7;

        int r1 = r + dir;
        if (inB(r1, c) && b[r1][c] == null) {
            addPawnMove(moves, r, c, r1, c, color, promoRow, false);
            int r2 = r + 2 * dir;
            if (r == startRow && b[r2][c] == null) {
                moves.add(new Move(r, c, r2, c, (char) 0, false, false));
            }
        }
        for (int dc : new int[]{-1, 1}) {
            int nc = c + dc;
            if (!inB(r1, nc)) continue;
            Piece target = b[r1][nc];
            if (target != null && target.color == opp(color)) {
                addPawnMove(moves, r, c, r1, nc, color, promoRow, false);
            } else if (ep != null && ep[0] == r1 && ep[1] == nc) {
                moves.add(new Move(r, c, r1, nc, (char) 0, false, true));
            }
        }
    }

    private void addPawnMove(List<Move> moves, int fr, int fc, int tr, int tc, char color, int promoRow, boolean ep) {
        if (tr == promoRow) {
            for (char promo : new char[]{'Q', 'R', 'B', 'N'}) {
                moves.add(new Move(fr, fc, tr, tc, promo, false, false));
            }
        } else {
            moves.add(new Move(fr, fc, tr, tc, (char) 0, false, ep));
        }
    }

    private void stepMoves(List<Move> moves, Piece[][] b, int r, int c, char color, int[][] offsets) {
        for (int[] o : offsets) {
            int nr = r + o[0], nc = c + o[1];
            if (!inB(nr, nc)) continue;
            Piece t = b[nr][nc];
            if (t == null || t.color == opp(color)) {
                moves.add(new Move(r, c, nr, nc, (char) 0, false, false));
            }
        }
    }

    private void slideMoves(List<Move> moves, Piece[][] b, int r, int c, char color, int[][] dirs) {
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (inB(nr, nc)) {
                Piece t = b[nr][nc];
                if (t == null) {
                    moves.add(new Move(r, c, nr, nc, (char) 0, false, false));
                } else {
                    if (t.color == opp(color)) moves.add(new Move(r, c, nr, nc, (char) 0, false, false));
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }
    }

    private void castlingMoves(List<Move> moves, Piece[][] b, int r, int c, char color) {
        boolean kingSide, queenSide;
        int row = (color == WHITE) ? 7 : 0;
        if (r != row || c != 4) return; // re non nella casa iniziale
        if (color == WHITE) { kingSide = castleWK; queenSide = castleWQ; }
        else { kingSide = castleBK; queenSide = castleBQ; }
        if (isInCheck(b, color)) return; // non si arrocca sotto scacco

        if (kingSide && b[row][5] == null && b[row][6] == null
                && b[row][7] != null && b[row][7].type == 'R' && b[row][7].color == color
                && !attacked(b, row, 5, opp(color)) && !attacked(b, row, 6, opp(color))) {
            moves.add(new Move(row, 4, row, 6, (char) 0, true, false));
        }
        if (queenSide && b[row][3] == null && b[row][2] == null && b[row][1] == null
                && b[row][0] != null && b[row][0].type == 'R' && b[row][0].color == color
                && !attacked(b, row, 3, opp(color)) && !attacked(b, row, 2, opp(color))) {
            moves.add(new Move(row, 4, row, 2, (char) 0, true, false));
        }
    }

    // --- Scacco / attacchi --------------------------------------------------

    private boolean isInCheck(Piece[][] b, char color) {
        int[] king = findKing(b, color);
        if (king == null) return false;
        return attacked(b, king[0], king[1], opp(color));
    }

    private int[] findKing(Piece[][] b, char color) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = b[r][c];
                if (p != null && p.color == color && p.type == 'K') return new int[]{r, c};
            }
        return null;
    }

    /** La casa (r,c) è attaccata da un pezzo di colore {@code by}? */
    private boolean attacked(Piece[][] b, int r, int c, char by) {
        // Pedoni: un pedone di colore 'by' attacca in avanti in diagonale.
        int pawnDir = (by == WHITE) ? -1 : 1; // direzione di movimento del pedone 'by'
        for (int dc : new int[]{-1, 1}) {
            int pr = r - pawnDir; // la casa da cui un pedone 'by' attaccherebbe (r,c)
            int pc = c + dc;
            if (inB(pr, pc)) {
                Piece p = b[pr][pc];
                if (p != null && p.color == by && p.type == 'P') return true;
            }
        }
        // Cavalli
        for (int[] o : KNIGHT) {
            int nr = r + o[0], nc = c + o[1];
            if (inB(nr, nc)) {
                Piece p = b[nr][nc];
                if (p != null && p.color == by && p.type == 'N') return true;
            }
        }
        // Re
        for (int[] o : KING) {
            int nr = r + o[0], nc = c + o[1];
            if (inB(nr, nc)) {
                Piece p = b[nr][nc];
                if (p != null && p.color == by && p.type == 'K') return true;
            }
        }
        // Diagonali (alfiere/donna)
        for (int[] d : DIAG) {
            int nr = r + d[0], nc = c + d[1];
            while (inB(nr, nc)) {
                Piece p = b[nr][nc];
                if (p != null) {
                    if (p.color == by && (p.type == 'B' || p.type == 'Q')) return true;
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }
        // Ortogonali (torre/donna)
        for (int[] d : ORTHO) {
            int nr = r + d[0], nc = c + d[1];
            while (inB(nr, nc)) {
                Piece p = b[nr][nc];
                if (p != null) {
                    if (p.color == by && (p.type == 'R' || p.type == 'Q')) return true;
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }
        return false;
    }

    private Piece[][] copy(Piece[][] src) {
        Piece[][] dst = new Piece[8][8];
        for (int r = 0; r < 8; r++) System.arraycopy(src[r], 0, dst[r], 0, 8);
        return dst;
    }

    // --- Lettura stato ------------------------------------------------------

    public synchronized Status status() { return status; }
    public synchronized Character winner() { return winner; }
    public synchronized Map<String, Character> seats() {
        Map<String, Character> m = new LinkedHashMap<>(seats);
        return m;
    }
    public synchronized boolean inCheck() { return isInCheck(board, turn); }
    public synchronized Character drawOfferBy() { return drawOfferBy; }
    public synchronized boolean timed() { return timed; }

    /** Orologio live: tempo residuo in ms, scalando il tempo trascorso al giocatore di turno. */
    public synchronized Map<String, Object> clockView() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timed", timed);
        if (!timed) return m;
        long w = whiteMillis, b = blackMillis;
        if (status == Status.PLAYING) {
            long elapsed = Duration.between(turnStartedAt, Instant.now()).toMillis();
            if (turn == WHITE) w = Math.max(0, w - elapsed);
            else b = Math.max(0, b - elapsed);
        }
        m.put("white", w);
        m.put("black", b);
        return m;
    }

    /** Board serializzabile: 8x8 di stringhe tipo "WK","BP" o null. */
    public synchronized List<List<String>> board() {
        List<List<String>> grid = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            List<String> row = new ArrayList<>();
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                row.add(p == null ? null : ("" + p.color + p.type));
            }
            grid.add(row);
        }
        return grid;
    }

    /** Mosse legali del giocatore di turno come mappe {fr,fc,tr,tc,promotion}. */
    public synchronized List<Map<String, Object>> legalMovesView() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Move m : legalMoves(turn)) {
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("fr", m.fr);
            mm.put("fc", m.fc);
            mm.put("tr", m.tr);
            mm.put("tc", m.tc);
            mm.put("promotion", m.promotion == 0 ? null : String.valueOf(m.promotion));
            out.add(mm);
        }
        return out;
    }
}
