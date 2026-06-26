package it.digitaliasistemi.minigames.game.battleship;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stato di una partita di Battaglia Navale. Logica pura, nessuna dipendenza da framework.
 * 2 giocatori. Colonne fisse ({@link #COLS}); le RIGHE sono per-giocatore e possono
 * crescere col potere "Espandi tabellone". La flotta base è {@link #BASE_FLEET} ma può
 * cambiare con i poteri (espandi barca, nave extra).
 *
 * <p>Informazione nascosta: lo stato completo (incluse le navi) NON va mai in broadcast.
 * L'engine costruisce per ciascun giocatore una vista personalizzata.
 */
public class BattleshipState {

    public enum Phase { PLACEMENT, BATTLE }

    public enum Status { PLAYING, WON }

    /** Colonne fisse. Le righe sono per-giocatore (vedi PlayerState.rows). */
    public static final int COLS = 10;
    public static final int BASE_ROWS = 10;
    /** Righe aggiunte dal potere "Espandi tabellone". */
    public static final int EXPAND_ROWS_STEP = 3;
    /** Lunghezza della nave aggiunta dal potere "Nave extra". */
    public static final int EXTRA_SHIP_LEN = 3;
    /** Flotta iniziale per giocatore (corazzata, incrociatore, 2 cacciatorpediniere, sommergibile). */
    public static final int[] BASE_FLEET = {5, 4, 3, 3, 2};

    /** Una nave piazzata: occupa una serie di celle contigue. Mutabile (poteri sposta/espandi). */
    public static final class Ship {
        int r;
        int c;
        int len;
        boolean horizontal;
        boolean[] hits;

        Ship(int r, int c, int len, boolean horizontal) {
            this.r = r;
            this.c = c;
            this.len = len;
            this.horizontal = horizontal;
            this.hits = new boolean[len];
        }

        int cellR(int i) { return horizontal ? r : r + i; }
        int cellC(int i) { return horizontal ? c + i : c; }

        boolean covers(int rr, int cc) {
            for (int i = 0; i < len; i++) {
                if (cellR(i) == rr && cellC(i) == cc) return true;
            }
            return false;
        }

        boolean isSunk() {
            for (boolean h : hits) if (!h) return false;
            return true;
        }
    }

    /** Stato per-giocatore: dimensione board, flotta, spari ricevuti, esche. */
    private static final class PlayerState {
        int rows = BASE_ROWS;
        final List<Ship> ships = new ArrayList<>();
        boolean[][] shotsReceived = new boolean[BASE_ROWS][COLS];
        boolean ready = false;
        /** Celle-esca attive (codificate r*COLS+c). Deviano il colpo avversario. */
        final Set<Integer> decoys = new HashSet<>();
    }

    private final String player1;
    private final String player2;
    private final Map<String, PlayerState> states = new LinkedHashMap<>();

    private Phase phase = Phase.PLACEMENT;
    private String currentTurn = null;
    private Status status = Status.PLAYING;
    private String winner = null;

    private final SecureRandom rnd = new SecureRandom();

    public BattleshipState(String player1, String player2) {
        this.player1 = player1;
        this.player2 = player2;
        states.put(player1, new PlayerState());
        states.put(player2, new PlayerState());
    }

    // ---- Piazzamento ----

    /** Piazza una flotta esplicita. Le lunghezze devono corrispondere alla flotta ATTESA del giocatore. */
    public synchronized boolean placeFleet(String player, List<int[]> ships) {
        if (phase != Phase.PLACEMENT) return false;
        PlayerState ps = states.get(player);
        if (ps == null) return false;
        if (ships == null) return false;

        // La flotta attesa è quella base più eventuali navi extra già concesse: per semplicità in
        // piazzamento accettiamo esattamente la flotta base (le navi extra si aggiungono coi poteri,
        // dopo). Quindi qui validiamo contro BASE_FLEET.
        if (ships.size() != BASE_FLEET.length) return false;

        List<Ship> candidate = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>();
        for (int len : BASE_FLEET) remaining.add(len);

        for (int[] spec : ships) {
            if (spec == null || spec.length < 4) return false;
            int r = spec[0], c = spec[1], len = spec[2];
            boolean horizontal = spec[3] != 0;
            if (!remaining.remove(Integer.valueOf(len))) return false;
            Ship s = new Ship(r, c, len, horizontal);
            if (!fits(ps, s)) return false;
            if (overlaps(candidate, s, null)) return false;
            candidate.add(s);
        }
        if (!remaining.isEmpty()) return false;

        ps.ships.clear();
        ps.ships.addAll(candidate);
        ps.ready = false;
        return true;
    }

    /** Piazza la flotta base casualmente. */
    public synchronized boolean randomize(String player) {
        if (phase != Phase.PLACEMENT) return false;
        PlayerState ps = states.get(player);
        if (ps == null) return false;

        List<Ship> placed = new ArrayList<>();
        for (int len : BASE_FLEET) {
            if (!placeRandom(ps, placed, len)) return false;
        }
        ps.ships.clear();
        ps.ships.addAll(placed);
        ps.ready = false;
        return true;
    }

    /** Prova a piazzare casualmente una nave di lunghezza len tra quelle già in {@code placed}. */
    private boolean placeRandom(PlayerState ps, List<Ship> placed, int len) {
        for (int attempt = 0; attempt < 2000; attempt++) {
            boolean horizontal = rnd.nextBoolean();
            int maxR = horizontal ? ps.rows : ps.rows - len;
            int maxC = horizontal ? COLS - len : COLS;
            if (maxR <= 0 || maxC <= 0) continue;
            int r = rnd.nextInt(maxR);
            int c = rnd.nextInt(maxC);
            Ship s = new Ship(r, c, len, horizontal);
            if (fits(ps, s) && !overlaps(placed, s, null)) {
                placed.add(s);
                return true;
            }
        }
        return false;
    }

    /** True se la nave sta interamente dentro la griglia del giocatore. */
    private boolean fits(PlayerState ps, Ship s) {
        if (s.len <= 0 || s.r < 0 || s.c < 0) return false;
        int endR = s.horizontal ? s.r : s.r + s.len - 1;
        int endC = s.horizontal ? s.c + s.len - 1 : s.c;
        return endR < ps.rows && endC < COLS;
    }

    /** True se s si sovrappone a una delle navi esistenti (esclusa {@code ignore}). */
    private boolean overlaps(List<Ship> existing, Ship s, Ship ignore) {
        for (int i = 0; i < s.len; i++) {
            int rr = s.cellR(i), cc = s.cellC(i);
            for (Ship o : existing) {
                if (o == ignore) continue;
                if (o.covers(rr, cc)) return true;
            }
        }
        return false;
    }

    public synchronized boolean ready(String player) {
        if (phase != Phase.PLACEMENT) return false;
        PlayerState ps = states.get(player);
        if (ps == null || !hasFullFleet(ps)) return false;
        ps.ready = true;
        if (states.get(player1).ready && states.get(player2).ready) {
            phase = Phase.BATTLE;
            currentTurn = player1;
        }
        return true;
    }

    /** In piazzamento basta avere la flotta base; le navi extra si aggiungono dopo coi poteri. */
    private boolean hasFullFleet(PlayerState ps) {
        if (ps.ships.size() < BASE_FLEET.length) return false;
        List<Integer> remaining = new ArrayList<>();
        for (int len : BASE_FLEET) remaining.add(len);
        for (Ship s : ps.ships) {
            remaining.remove(Integer.valueOf(s.len));
        }
        return remaining.isEmpty();
    }

    // ---- Battaglia ----

    public enum FireResult { INVALID, MISS, HIT, SUNK, WIN }

    /** Esito di uno sparo + prossimità (per il siluro): proximity = distanza Chebyshev dalla nave più vicina (-1 se n/d). */
    public record FireOutcome(FireResult result, int proximity) {}

    /** Sparo normale. */
    public synchronized FireResult fire(String player, int r, int c) {
        return fireInternal(player, r, c).result();
    }

    /** Sparo "siluro": come fire ma calcola anche la prossimità della nave più vicina al punto di impatto. */
    public synchronized FireOutcome fireProximity(String player, int r, int c) {
        return fireInternal(player, r, c);
    }

    private FireOutcome fireInternal(String player, int r, int c) {
        if (status != Status.PLAYING || phase != Phase.BATTLE) return new FireOutcome(FireResult.INVALID, -1);
        if (!player.equals(currentTurn)) return new FireOutcome(FireResult.INVALID, -1);
        String enemy = opponentOf(player);
        if (enemy == null) return new FireOutcome(FireResult.INVALID, -1);
        PlayerState target = states.get(enemy);
        if (r < 0 || r >= target.rows || c < 0 || c >= COLS) return new FireOutcome(FireResult.INVALID, -1);
        if (target.shotsReceived[r][c]) return new FireOutcome(FireResult.INVALID, -1);

        int proximity = nearestShipDistance(target, r, c);

        // Esca: se la cella bersaglio è marcata, devia il colpo su una cella-nave casuale del target.
        if (target.decoys.contains(r * COLS + c)) {
            target.decoys.remove(r * COLS + c);
            target.shotsReceived[r][c] = true; // l'avversario vede MISS qui
            FireResult redirected = redirectHit(target);
            if (redirected == FireResult.WIN) {
                status = Status.WON;
                winner = player;
                currentTurn = null;
                return new FireOutcome(FireResult.WIN, proximity);
            }
            currentTurn = enemy;
            return new FireOutcome(FireResult.MISS, proximity); // per l'avversario è un buco nell'acqua
        }

        target.shotsReceived[r][c] = true;
        Ship hitShip = shipAt(target, r, c);

        FireResult result;
        if (hitShip == null) {
            result = FireResult.MISS;
        } else {
            markHit(hitShip, r, c);
            if (allSunk(target)) {
                status = Status.WON;
                winner = player;
                currentTurn = null;
                return new FireOutcome(FireResult.WIN, proximity);
            }
            result = hitShip.isSunk() ? FireResult.SUNK : FireResult.HIT;
        }
        currentTurn = enemy;
        return new FireOutcome(result, proximity);
    }

    /** Colpisce una cella-nave casuale non ancora colpita del target. Ritorna l'esito (per l'usante). */
    private FireResult redirectHit(PlayerState target) {
        List<int[]> candidates = new ArrayList<>();
        for (Ship s : target.ships) {
            for (int i = 0; i < s.len; i++) {
                if (!s.hits[i]) candidates.add(new int[]{s.cellR(i), s.cellC(i)});
            }
        }
        if (candidates.isEmpty()) return FireResult.MISS;
        int[] cell = candidates.get(rnd.nextInt(candidates.size()));
        target.shotsReceived[cell[0]][cell[1]] = true;
        Ship s = shipAt(target, cell[0], cell[1]);
        if (s != null) markHit(s, cell[0], cell[1]);
        if (allSunk(target)) return FireResult.WIN;
        return FireResult.HIT;
    }

    private void markHit(Ship s, int r, int c) {
        for (int i = 0; i < s.len; i++) {
            if (s.cellR(i) == r && s.cellC(i) == c) { s.hits[i] = true; return; }
        }
    }

    private Ship shipAt(PlayerState ps, int r, int c) {
        for (Ship s : ps.ships) if (s.covers(r, c)) return s;
        return null;
    }

    /** Distanza Chebyshev minima da (r,c) a una qualsiasi cella-nave (0 se c'è una nave proprio lì). */
    private int nearestShipDistance(PlayerState target, int r, int c) {
        int best = Integer.MAX_VALUE;
        for (Ship s : target.ships) {
            for (int i = 0; i < s.len; i++) {
                int d = Math.max(Math.abs(s.cellR(i) - r), Math.abs(s.cellC(i) - c));
                if (d < best) best = d;
            }
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private boolean allSunk(PlayerState ps) {
        for (Ship s : ps.ships) if (!s.isSunk()) return false;
        return !ps.ships.isEmpty();
    }

    private String opponentOf(String player) {
        if (player.equals(player1)) return player2;
        if (player.equals(player2)) return player1;
        return null;
    }

    // ---- Poteri ----

    /** Radar: contenuto reale delle celle a distanza <=1 da (r,c) sulla board NEMICA. Solo lettura. */
    public synchronized List<int[]> radarPeek(String player, int r, int c) {
        List<int[]> out = new ArrayList<>();
        if (phase != Phase.BATTLE) return out;
        String enemy = opponentOf(player);
        if (enemy == null) return out;
        PlayerState target = states.get(enemy);
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int rr = r + dr, cc = c + dc;
                if (rr < 0 || rr >= target.rows || cc < 0 || cc >= COLS) continue;
                boolean ship = shipAt(target, rr, cc) != null;
                out.add(new int[]{rr, cc, ship ? 1 : 0});
            }
        }
        return out;
    }

    /** Sposta una propria nave (identificata da una sua cella) in una nuova posizione/orientamento. */
    public synchronized boolean moveShip(String player, int fromR, int fromC, int toR, int toC, boolean horizontal) {
        PlayerState ps = states.get(player);
        if (ps == null || status != Status.PLAYING) return false;
        Ship ship = shipAt(ps, fromR, fromC);
        if (ship == null) return false;
        Ship moved = new Ship(toR, toC, ship.len, horizontal);
        if (!fits(ps, moved)) return false;
        if (overlaps(ps.ships, moved, ship)) return false;
        ship.r = toR; ship.c = toC; ship.horizontal = horizontal;
        recomputeHits(ps, ship);
        return true;
    }

    /** Allunga di 1 una propria nave (in coda o in testa, dove c'è spazio libero). */
    public synchronized boolean extendShip(String player, int r, int c) {
        PlayerState ps = states.get(player);
        if (ps == null || status != Status.PLAYING) return false;
        Ship ship = shipAt(ps, r, c);
        if (ship == null) return false;
        // Prova ad allungare in coda (mantiene r,c di testa, len+1)
        Ship tail = new Ship(ship.r, ship.c, ship.len + 1, ship.horizontal);
        if (fits(ps, tail) && !overlaps(ps.ships, tail, ship)) {
            ship.len += 1;
            ship.hits = growHits(ship.hits, false);
            recomputeHits(ps, ship);
            return true;
        }
        // Altrimenti allunga in testa (sposta testa di una cella indietro)
        int newR = ship.horizontal ? ship.r : ship.r - 1;
        int newC = ship.horizontal ? ship.c - 1 : ship.c;
        Ship head = new Ship(newR, newC, ship.len + 1, ship.horizontal);
        if (newR >= 0 && newC >= 0 && fits(ps, head) && !overlaps(ps.ships, head, ship)) {
            ship.r = newR; ship.c = newC; ship.len += 1;
            ship.hits = growHits(ship.hits, true); // nuova cella in testa
            recomputeHits(ps, ship);
            return true;
        }
        return false;
    }

    /** Espande il proprio tabellone di EXPAND_ROWS_STEP righe in basso. */
    public synchronized boolean expandBoard(String player) {
        PlayerState ps = states.get(player);
        if (ps == null || status != Status.PLAYING) return false;
        int newRows = ps.rows + EXPAND_ROWS_STEP;
        boolean[][] grid = new boolean[newRows][COLS];
        for (int r = 0; r < ps.rows; r++) System.arraycopy(ps.shotsReceived[r], 0, grid[r], 0, COLS);
        ps.shotsReceived = grid;
        ps.rows = newRows;
        return true;
    }

    /** Aggiunge una nave extra (lunghezza EXTRA_SHIP_LEN) in uno spazio libero. */
    public synchronized boolean addExtraShip(String player) {
        PlayerState ps = states.get(player);
        if (ps == null || status != Status.PLAYING) return false;
        return placeRandom(ps, ps.ships, EXTRA_SHIP_LEN);
    }

    /** Marca una cella di una propria nave come esca (devia il prossimo colpo avversario su quella cella). */
    public synchronized boolean placeDecoy(String player, int r, int c) {
        PlayerState ps = states.get(player);
        if (ps == null || status != Status.PLAYING) return false;
        if (shipAt(ps, r, c) == null) return false; // deve essere una propria nave
        ps.decoys.add(r * COLS + c);
        return true;
    }

    /** Ricalcola gli hit di una nave dalle celle effettivamente già bersagliate (coerenza dopo sposta/espandi). */
    private void recomputeHits(PlayerState ps, Ship ship) {
        for (int i = 0; i < ship.len; i++) {
            int rr = ship.cellR(i), cc = ship.cellC(i);
            ship.hits[i] = rr >= 0 && rr < ps.rows && cc >= 0 && cc < COLS && ps.shotsReceived[rr][cc];
        }
    }

    private boolean[] growHits(boolean[] hits, boolean atHead) {
        boolean[] out = new boolean[hits.length + 1];
        if (atHead) System.arraycopy(hits, 0, out, 1, hits.length);
        else System.arraycopy(hits, 0, out, 0, hits.length);
        return out;
    }

    // ---- Viste / accessori ----

    /** Vista della PROPRIA board: "~" acqua, "S" nave, "X" nave colpita, "O" miss subito. */
    public synchronized String[][] viewBoardOwn(String player) {
        PlayerState ps = states.get(player);
        if (ps == null) return new String[0][0];
        String[][] grid = new String[ps.rows][COLS];
        for (int r = 0; r < ps.rows; r++) for (int c = 0; c < COLS; c++) grid[r][c] = "~";
        for (Ship s : ps.ships) {
            for (int i = 0; i < s.len; i++) grid[s.cellR(i)][s.cellC(i)] = "S";
        }
        for (int r = 0; r < ps.rows; r++) {
            for (int c = 0; c < COLS; c++) {
                if (ps.shotsReceived[r][c]) grid[r][c] = "S".equals(grid[r][c]) ? "X" : "O";
            }
        }
        return grid;
    }

    /** Vista della board AVVERSARIA: "?" ignoto, "X" colpito, "O" mancato. Dimensione = righe avversario. */
    public synchronized String[][] viewBoardEnemy(String player) {
        String enemy = opponentOf(player);
        if (enemy == null) return new String[0][0];
        PlayerState target = states.get(enemy);
        String[][] grid = new String[target.rows][COLS];
        for (int r = 0; r < target.rows; r++) for (int c = 0; c < COLS; c++) grid[r][c] = "?";
        for (int r = 0; r < target.rows; r++) {
            for (int c = 0; c < COLS; c++) {
                if (!target.shotsReceived[r][c]) continue;
                grid[r][c] = shipAt(target, r, c) != null ? "X" : "O";
            }
        }
        return grid;
    }

    /** Celle-esca del giocatore (per mostrarle solo a lui). */
    public synchronized List<int[]> decoyCells(String player) {
        PlayerState ps = states.get(player);
        List<int[]> out = new ArrayList<>();
        if (ps == null) return out;
        for (int code : ps.decoys) out.add(new int[]{code / COLS, code % COLS});
        return out;
    }

    public synchronized int rowsOf(String player) {
        PlayerState ps = states.get(player);
        return ps != null ? ps.rows : BASE_ROWS;
    }

    public synchronized int sunkCount(String player) {
        PlayerState ps = states.get(player);
        if (ps == null) return 0;
        int n = 0;
        for (Ship s : ps.ships) if (s.isSunk()) n++;
        return n;
    }

    public synchronized int fleetCount(String player) {
        PlayerState ps = states.get(player);
        return ps != null ? ps.ships.size() : 0;
    }

    public synchronized boolean isReady(String player) {
        PlayerState ps = states.get(player);
        return ps != null && ps.ready;
    }

    public synchronized boolean hasFleet(String player) {
        PlayerState ps = states.get(player);
        return ps != null && hasFullFleet(ps);
    }

    public synchronized Phase phase() { return phase; }
    public synchronized Status status() { return status; }
    public synchronized String winner() { return winner; }
    public synchronized String currentTurn() { return currentTurn; }
    public String player1() { return player1; }
    public String player2() { return player2; }
    public String opponent(String player) { return opponentOf(player); }
}
