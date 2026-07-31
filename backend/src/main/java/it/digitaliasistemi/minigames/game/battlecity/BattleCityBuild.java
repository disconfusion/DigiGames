package it.digitaliasistemi.minigames.game.battlecity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Construction Mode: la mappa la disegnano i giocatori, ognuno la propria zona, prima di giocarci.
 *
 * <p>Stato puro come il resto del gioco (nessun framework, testabile con JUnit). La griglia è
 * {@value BattleCityMaps#TILES}×{@value BattleCityMaps#TILES} tile, gli stessi caratteri delle
 * mappe del gioco.
 *
 * <p><b>Zone.</b> Con due giocatori il campo si divide in verticale: al primo le colonne di
 * sinistra, al secondo quelle di destra, e la <b>colonna centrale resta libera</b> — è il
 * corridoio che garantisce il passaggio da una metà all'altra. Con un solo giocatore si disegna
 * tutto. Le prime {@value #RESERVED_TOP} righe (dove compaiono i nemici) e le ultime
 * {@value #RESERVED_BOTTOM} (dove sta l'aquila) non sono dipingibili: così nessuna mappa può
 * murare la base o bloccare le comparse.
 */
public class BattleCityBuild {

    /** Caratteri che un giocatore può usare (l'aquila la piazza il gioco). */
    public static final String PAINTABLE = ".BSWTI";
    /** Righe in alto riservate alle comparse dei nemici. */
    public static final int RESERVED_TOP = 2;
    /** Righe in basso riservate alla base e alla sua corona. */
    public static final int RESERVED_BOTTOM = 2;

    private final List<String> players;
    private final char[][] tiles = new char[BattleCityMaps.TILES][BattleCityMaps.TILES];
    private final Set<String> ready = new LinkedHashSet<>();

    public BattleCityBuild(List<String> players) {
        this.players = List.copyOf(players);
        for (char[] row : tiles) java.util.Arrays.fill(row, '.');
    }

    /** Indice della colonna centrale, sempre libera e di nessuno. */
    public static int middleColumn() {
        return BattleCityMaps.TILES / 2;
    }

    /** La cella è dipingibile da qualcuno? (fuori dalle righe riservate e dalla colonna centrale) */
    public static boolean isPaintable(int row, int col) {
        if (row < 0 || col < 0 || row >= BattleCityMaps.TILES || col >= BattleCityMaps.TILES) return false;
        if (row < RESERVED_TOP || row >= BattleCityMaps.TILES - RESERVED_BOTTOM) return false;
        return col != middleColumn();
    }

    /**
     * Zona del giocatore: 0 = metà sinistra, 1 = metà destra. Con un solo giocatore la zona è
     * tutto il campo. Chi non è in partita non ha zona (-1).
     */
    public int zoneOf(String username) {
        int idx = players.indexOf(username);
        if (idx < 0) return -1;
        return players.size() <= 1 ? -1 : idx % 2;
    }

    /** Il giocatore può dipingere questa cella? */
    public boolean canPaint(String username, int row, int col) {
        if (!players.contains(username) || !isPaintable(row, col)) return false;
        int zone = zoneOf(username);
        if (zone < 0) return true;                       // giocatore unico: tutto suo
        return zone == 0 ? col < middleColumn() : col > middleColumn();
    }

    /**
     * Dipinge una cella. Ritorna true se la griglia è cambiata (il chiamante trasmette).
     * Le celle fuori zona, le righe riservate e i caratteri sconosciuti vengono ignorati:
     * il server non si fida di quello che arriva dal client.
     */
    public synchronized boolean paint(String username, int row, int col, char tile) {
        if (PAINTABLE.indexOf(tile) < 0) return false;
        if (!canPaint(username, row, col)) return false;
        if (tiles[row][col] == tile) return false;
        tiles[row][col] = tile;
        ready.remove(username); // ha ripreso a disegnare: il "pronto" decade
        return true;
    }

    /** Riempie la propria zona con una bozza casuale (comodo per non partire dal foglio bianco). */
    public synchronized boolean fillRandom(String username, Random rnd) {
        if (!players.contains(username)) return false;
        boolean changed = false;
        for (int r = 0; r < BattleCityMaps.TILES; r++) {
            for (int c = 0; c < BattleCityMaps.TILES; c++) {
                if (!canPaint(username, r, c)) continue;
                double p = rnd.nextDouble();
                char t = p < 0.34 ? 'B' : p < 0.46 ? 'S' : p < 0.56 ? 'W' : p < 0.62 ? 'T' : p < 0.66 ? 'I' : '.';
                if (tiles[r][c] != t) {
                    tiles[r][c] = t;
                    changed = true;
                }
            }
        }
        if (changed) ready.remove(username);
        return changed;
    }

    /** Svuota la propria zona. */
    public synchronized boolean clearZone(String username) {
        if (!players.contains(username)) return false;
        boolean changed = false;
        for (int r = 0; r < BattleCityMaps.TILES; r++) {
            for (int c = 0; c < BattleCityMaps.TILES; c++) {
                if (canPaint(username, r, c) && tiles[r][c] != '.') {
                    tiles[r][c] = '.';
                    changed = true;
                }
            }
        }
        if (changed) ready.remove(username);
        return changed;
    }

    /**
     * Copia la propria zona a specchio nell'altra metà. Utile quando si gioca da soli o quando
     * l'altro non ha ancora disegnato; non tocca le celle riservate.
     */
    public synchronized boolean mirror(String username) {
        if (!players.contains(username)) return false;
        int mid = middleColumn();
        boolean changed = false;
        for (int r = 0; r < BattleCityMaps.TILES; r++) {
            for (int c = 0; c < BattleCityMaps.TILES; c++) {
                if (!canPaint(username, r, c)) continue;
                int target = BattleCityMaps.TILES - 1 - c;
                if (target == mid || !isPaintable(r, target)) continue;
                if (tiles[r][target] != tiles[r][c]) {
                    tiles[r][target] = tiles[r][c];
                    changed = true;
                }
            }
        }
        return changed;
    }

    public synchronized void setReady(String username, boolean value) {
        if (!players.contains(username)) return;
        if (value) ready.add(username);
        else ready.remove(username);
    }

    public synchronized boolean isReady(String username) {
        return ready.contains(username);
    }

    /** Tutti i giocatori presenti hanno dichiarato di aver finito? */
    public synchronized boolean everyoneReady() {
        return !players.isEmpty() && ready.containsAll(players);
    }

    /** La mappa disegnata, nel formato delle mappe del gioco. */
    public synchronized String[] rows() {
        String[] out = new String[BattleCityMaps.TILES];
        for (int r = 0; r < BattleCityMaps.TILES; r++) out[r] = new String(tiles[r]);
        return out;
    }

    /** Snapshot per il frontend: griglia, zone, stato dei "pronto". */
    public synchronized Map<String, Object> view() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("grid", List.of(rows()));
        m.put("middle", middleColumn());
        m.put("reservedTop", RESERVED_TOP);
        m.put("reservedBottom", RESERVED_BOTTOM);
        Map<String, Object> zones = new LinkedHashMap<>();
        Map<String, Object> readyMap = new LinkedHashMap<>();
        for (String p : players) {
            zones.put(p, zoneOf(p));
            readyMap.put(p, ready.contains(p));
        }
        m.put("zones", zones);
        m.put("ready", readyMap);
        m.put("everyoneReady", everyoneReady());
        return m;
    }

    /**
     * La mappa è giocabile? Serve che dai punti di comparsa dei nemici si arrivi alla base
     * camminando (i muri distruttibili contano come passaggi: si aprono a colpi; acciaio e acqua
     * no). Le mappe che chiudono la base in una gabbia d'acciaio vengono rifiutate.
     */
    public static boolean isPlayable(String[] rows) {
        if (rows == null || rows.length != BattleCityMaps.TILES) return false;
        for (String row : rows) {
            if (row == null || row.length() != BattleCityMaps.TILES) return false;
            for (char ch : row.toCharArray()) {
                if (PAINTABLE.indexOf(ch) < 0) return false;
            }
        }
        int n = BattleCityMaps.TILES;
        int baseRow = n - 1, baseCol = n / 2;
        boolean[][] seen = new boolean[n][n];
        Deque<int[]> queue = new ArrayDeque<>();
        for (int col : new int[] {0, n / 2, n - 1}) {
            if (passable(rows, 0, col)) {
                queue.add(new int[] {0, col});
                seen[0][col] = true;
            }
        }
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            if (cur[0] == baseRow && Math.abs(cur[1] - baseCol) <= 1) return true;
            for (int[] d : new int[][] { {1, 0}, {-1, 0}, {0, 1}, {0, -1} }) {
                int r = cur[0] + d[0], c = cur[1] + d[1];
                if (r < 0 || c < 0 || r >= n || c >= n || seen[r][c] || !passable(rows, r, c)) continue;
                seen[r][c] = true;
                queue.add(new int[] {r, c});
            }
        }
        return false;
    }

    /** Attraversabile a colpi di cannone: tutto tranne acciaio e acqua. */
    private static boolean passable(String[] rows, int r, int c) {
        char ch = rows[r].charAt(c);
        return ch != 'S' && ch != 'W';
    }

    /** Normalizza una mappa che arriva da fuori (righe mancanti, caratteri strani, zone riservate). */
    public static String[] sanitize(String[] rows) {
        String[] out = new String[BattleCityMaps.TILES];
        for (int r = 0; r < BattleCityMaps.TILES; r++) {
            StringBuilder sb = new StringBuilder(BattleCityMaps.TILES);
            String src = rows != null && r < rows.length && rows[r] != null ? rows[r] : "";
            for (int c = 0; c < BattleCityMaps.TILES; c++) {
                char ch = c < src.length() ? src.charAt(c) : '.';
                if (PAINTABLE.indexOf(ch) < 0) ch = '.';
                if (!isPaintable(r, c)) ch = '.';
                sb.append(ch);
            }
            out[r] = sb.toString();
        }
        return out;
    }

    public List<String> players() {
        return players;
    }

    /** Elenco dei giocatori che hanno finito (diagnostica e test). */
    public synchronized List<String> readyPlayers() {
        return new ArrayList<>(ready);
    }
}
