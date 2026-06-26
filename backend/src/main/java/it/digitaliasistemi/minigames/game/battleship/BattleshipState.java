package it.digitaliasistemi.minigames.game.battleship;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stato di una partita di Battaglia Navale. Logica pura, nessuna dipendenza da framework
 * (testabile senza CDI). 2 giocatori, board 10x10, flotta [5,4,3,3,2].
 *
 * <p>Informazione nascosta: lo stato completo (incluse le navi) NON va mai inviato in
 * broadcast. L'engine costruisce per ciascun giocatore una vista personalizzata leggendo
 * questo stato (vedi {@link #viewBoardOwn(String)} e {@link #viewBoardEnemy(String)}).
 */
public class BattleshipState {

    public enum Phase { PLACEMENT, BATTLE }

    public enum Status { PLAYING, WON }

    public static final int SIZE = 10;
    /** Lunghezze della flotta per giocatore (corazzata, incrociatore, 2 cacciatorpediniere, sommergibile). */
    public static final int[] FLEET = {5, 4, 3, 3, 2};

    /** Una nave piazzata: occupa una serie di celle contigue. */
    public static final class Ship {
        final int r;
        final int c;
        final int len;
        final boolean horizontal;
        /** hits[i] = true se la i-esima cella della nave è stata colpita. */
        final boolean[] hits;

        Ship(int r, int c, int len, boolean horizontal) {
            this.r = r;
            this.c = c;
            this.len = len;
            this.horizontal = horizontal;
            this.hits = new boolean[len];
        }

        /** True se (rr,cc) appartiene a questa nave. */
        boolean covers(int rr, int cc) {
            for (int i = 0; i < len; i++) {
                int cellR = horizontal ? r : r + i;
                int cellC = horizontal ? c + i : c;
                if (cellR == rr && cellC == cc) return true;
            }
            return false;
        }

        boolean isSunk() {
            for (boolean h : hits) if (!h) return false;
            return true;
        }
    }

    /** Stato per-giocatore: flotta + griglia degli spari ricevuti. */
    private static final class PlayerState {
        final List<Ship> ships = new ArrayList<>();
        /** Spari ricevuti su questo giocatore: shotsReceived[r][c] = true se l'avversario ha già sparato lì. */
        final boolean[][] shotsReceived = new boolean[SIZE][SIZE];
        boolean ready = false;
    }

    private final String player1;
    private final String player2;
    private final Map<String, PlayerState> states = new LinkedHashMap<>();

    private Phase phase = Phase.PLACEMENT;
    private String currentTurn = null; // valorizzato all'inizio della fase BATTLE
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

    /**
     * Piazza una flotta esplicita per il giocatore. Sostituisce qualunque flotta precedente.
     * Valida: tutte dentro i bordi, nessuna sovrapposizione, lunghezze == FLEET (esatte).
     *
     * @return true se la flotta è valida e piazzata, false altrimenti (stato invariato).
     */
    public synchronized boolean placeFleet(String player, List<int[]> ships) {
        // ships: ognuna è {r, c, len, horizontal(0/1)}
        if (phase != Phase.PLACEMENT) return false;
        PlayerState ps = states.get(player);
        if (ps == null) return false;
        if (ships == null || ships.size() != FLEET.length) return false;

        List<Ship> candidate = new ArrayList<>();
        // Le lunghezze devono corrispondere ESATTAMENTE a FLEET (come multiset).
        List<Integer> remaining = new ArrayList<>();
        for (int len : FLEET) remaining.add(len);

        for (int[] spec : ships) {
            if (spec == null || spec.length < 4) return false;
            int r = spec[0];
            int c = spec[1];
            int len = spec[2];
            boolean horizontal = spec[3] != 0;
            if (!remaining.remove(Integer.valueOf(len))) return false; // lunghezza non prevista
            Ship s = new Ship(r, c, len, horizontal);
            if (!fits(s)) return false;
            if (overlaps(candidate, s)) return false;
            candidate.add(s);
        }
        if (!remaining.isEmpty()) return false; // flotta incompleta

        ps.ships.clear();
        ps.ships.addAll(candidate);
        ps.ready = false; // un nuovo piazzamento azzera il "pronto"
        return true;
    }

    /** Piazza una flotta valida casualmente (SecureRandom) per il giocatore. */
    public synchronized boolean randomize(String player) {
        if (phase != Phase.PLACEMENT) return false;
        PlayerState ps = states.get(player);
        if (ps == null) return false;

        List<Ship> placed = new ArrayList<>();
        for (int len : FLEET) {
            boolean ok = false;
            for (int attempt = 0; attempt < 1000 && !ok; attempt++) {
                boolean horizontal = rnd.nextBoolean();
                int maxR = horizontal ? SIZE : SIZE - len;
                int maxC = horizontal ? SIZE - len : SIZE;
                int r = rnd.nextInt(maxR);
                int c = rnd.nextInt(maxC);
                Ship s = new Ship(r, c, len, horizontal);
                if (fits(s) && !overlaps(placed, s)) {
                    placed.add(s);
                    ok = true;
                }
            }
            if (!ok) return false; // estremamente improbabile su 10x10
        }
        ps.ships.clear();
        ps.ships.addAll(placed);
        ps.ready = false;
        return true;
    }

    /** True se la nave sta interamente dentro la griglia. */
    private boolean fits(Ship s) {
        if (s.len <= 0) return false;
        if (s.r < 0 || s.c < 0) return false;
        int endR = s.horizontal ? s.r : s.r + s.len - 1;
        int endC = s.horizontal ? s.c + s.len - 1 : s.c;
        return endR < SIZE && endC < SIZE;
    }

    /** True se s si sovrappone a una delle navi già presenti. */
    private boolean overlaps(List<Ship> existing, Ship s) {
        for (int i = 0; i < s.len; i++) {
            int rr = s.horizontal ? s.r : s.r + i;
            int cc = s.horizontal ? s.c + i : s.c;
            for (Ship o : existing) {
                if (o.covers(rr, cc)) return true;
            }
        }
        return false;
    }

    /**
     * Marca il giocatore come pronto (solo se ha una flotta completa valida).
     * Quando ENTRAMBI sono pronti, passa alla fase BATTLE col turno al giocatore 1.
     *
     * @return true se il "pronto" è stato accettato.
     */
    public synchronized boolean ready(String player) {
        if (phase != Phase.PLACEMENT) return false;
        PlayerState ps = states.get(player);
        if (ps == null) return false;
        if (!hasFullFleet(ps)) return false;
        ps.ready = true;
        if (states.get(player1).ready && states.get(player2).ready) {
            phase = Phase.BATTLE;
            currentTurn = player1;
        }
        return true;
    }

    private boolean hasFullFleet(PlayerState ps) {
        if (ps.ships.size() != FLEET.length) return false;
        List<Integer> remaining = new ArrayList<>();
        for (int len : FLEET) remaining.add(len);
        for (Ship s : ps.ships) {
            if (!remaining.remove(Integer.valueOf(s.len))) return false;
        }
        return remaining.isEmpty();
    }

    // ---- Battaglia ----

    /** Esito di uno sparo. */
    public enum FireResult { INVALID, MISS, HIT, SUNK, WIN }

    /**
     * Il giocatore {@code player} spara in (r,c) sulla board dell'avversario.
     * Valido solo in fase BATTLE, nel proprio turno, su una cella mai colpita prima.
     * Il turno passa SEMPRE all'avversario (anche su hit). Vittoria quando tutte le
     * navi avversarie sono affondate.
     *
     * @return l'esito; INVALID se la mossa è illegale (stato invariato).
     */
    public synchronized FireResult fire(String player, int r, int c) {
        if (status != Status.PLAYING) return FireResult.INVALID;
        if (phase != Phase.BATTLE) return FireResult.INVALID;
        if (!player.equals(currentTurn)) return FireResult.INVALID;
        if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) return FireResult.INVALID;

        String enemy = opponentOf(player);
        if (enemy == null) return FireResult.INVALID;
        PlayerState target = states.get(enemy);
        if (target.shotsReceived[r][c]) return FireResult.INVALID; // già colpita

        target.shotsReceived[r][c] = true;

        Ship hitShip = null;
        for (Ship s : target.ships) {
            if (s.covers(r, c)) { hitShip = s; break; }
        }

        FireResult result;
        if (hitShip == null) {
            result = FireResult.MISS;
        } else {
            // segna l'hit nella cella corretta della nave
            for (int i = 0; i < hitShip.len; i++) {
                int cellR = hitShip.horizontal ? hitShip.r : hitShip.r + i;
                int cellC = hitShip.horizontal ? hitShip.c + i : hitShip.c;
                if (cellR == r && cellC == c) { hitShip.hits[i] = true; break; }
            }
            if (allSunk(target)) {
                status = Status.WON;
                winner = player;
                currentTurn = null;
                return FireResult.WIN;
            }
            result = hitShip.isSunk() ? FireResult.SUNK : FireResult.HIT;
        }

        // Turni alternati anche su hit: il turno passa sempre.
        currentTurn = enemy;
        return result;
    }

    private boolean allSunk(PlayerState ps) {
        for (Ship s : ps.ships) {
            if (!s.isSunk()) return false;
        }
        return true;
    }

    private String opponentOf(String player) {
        if (player.equals(player1)) return player2;
        if (player.equals(player2)) return player1;
        return null;
    }

    // ---- Viste / accessori ----

    /**
     * Vista della PROPRIA board per il giocatore (10x10):
     * "~" acqua, "S" tua nave, "X" tua nave colpita, "O" miss dell'avversario su di te.
     */
    public synchronized String[][] viewBoardOwn(String player) {
        PlayerState ps = states.get(player);
        String[][] grid = new String[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) grid[r][c] = "~";
        }
        if (ps == null) return grid;
        // navi
        for (Ship s : ps.ships) {
            for (int i = 0; i < s.len; i++) {
                int rr = s.horizontal ? s.r : s.r + i;
                int cc = s.horizontal ? s.c + i : s.c;
                grid[rr][cc] = "S";
            }
        }
        // spari ricevuti
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (ps.shotsReceived[r][c]) {
                    grid[r][c] = "S".equals(grid[r][c]) ? "X" : "O";
                }
            }
        }
        return grid;
    }

    /**
     * Vista della board AVVERSARIA per il giocatore (10x10), senza mai rivelare le navi
     * non colpite: "?" sconosciuto, "X" tuo colpo a segno, "O" tuo colpo mancato.
     */
    public synchronized String[][] viewBoardEnemy(String player) {
        String enemy = opponentOf(player);
        String[][] grid = new String[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) grid[r][c] = "?";
        }
        if (enemy == null) return grid;
        PlayerState target = states.get(enemy);
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (!target.shotsReceived[r][c]) continue;
                boolean hit = false;
                for (Ship s : target.ships) {
                    if (s.covers(r, c)) { hit = true; break; }
                }
                grid[r][c] = hit ? "X" : "O";
            }
        }
        return grid;
    }

    /** Numero di navi del giocatore già affondate (tutte le celle colpite). */
    public synchronized int sunkCount(String player) {
        PlayerState ps = states.get(player);
        if (ps == null) return 0;
        int n = 0;
        for (Ship s : ps.ships) if (s.isSunk()) n++;
        return n;
    }

    public synchronized boolean isReady(String player) {
        PlayerState ps = states.get(player);
        return ps != null && ps.ready;
    }

    /** True se il giocatore ha una flotta completa valida piazzata. */
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
