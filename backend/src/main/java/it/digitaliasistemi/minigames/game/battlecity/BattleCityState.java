package it.digitaliasistemi.minigames.game.battlecity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Stato puro di Battle City: nessun framework, tutta la logica qui e testabile con JUnit.
 *
 * <p><b>Geometria.</b> Il campo è {@value BattleCityMaps#TILES}×{@value BattleCityMaps#TILES} tile
 * da {@value #TILE} px, ma i mattoni si rompono a mezzo tile come nell'originale: la griglia dei
 * muri è quindi in <i>celle</i> da {@value #CELL} px ({@value #CELLS}×{@value #CELLS}). Tank
 * 16×16 px, proiettili 4×4 px, coordinate in px logici (il frontend scala sul canvas).
 *
 * <p><b>Modalità.</b> {@link Mode#COOP} = 1-2 giocatori contro {@value #ENEMIES_PER_LEVEL} tank
 * IA a ondate, con la base (aquila) da difendere. {@link Mode#DUEL} = duello fra due colleghi
 * sulla stessa mappa, senza nemici né base, primo a {@value #DUEL_HITS} colpi.
 *
 * <p>Server autoritativo: dai client arriva solo la direzione premuta e lo sparo.
 */
public class BattleCityState {

    public enum Mode { COOP, DUEL }
    public enum Status { PLAYING, WON, LOST }
    public enum Dir { UP, RIGHT, DOWN, LEFT }

    /** Contenuto di una cella della griglia dei muri. */
    public enum Cell {
        EMPTY, BRICK, STEEL, WATER, TREES, ICE, BASE;

        /** Blocca il movimento dei tank? (i cespugli e il ghiaccio no) */
        public boolean blocksTank() {
            return this == BRICK || this == STEEL || this == WATER || this == BASE;
        }

        /** Ferma i proiettili? (acqua, cespugli e ghiaccio no) */
        public boolean blocksBullet() {
            return this == BRICK || this == STEEL || this == BASE;
        }
    }

    /** Tipi di nemico dell'originale: normale, veloce, cannone rapido, corazzato. */
    public enum EnemyKind {
        BASIC(40, 1, 150), FAST(96, 1, 150), POWER(48, 1, 230), ARMOR(48, 4, 150);

        public final double speed;
        public final int hp;
        public final double bulletSpeed;

        EnemyKind(double speed, int hp, double bulletSpeed) {
            this.speed = speed;
            this.hp = hp;
            this.bulletSpeed = bulletSpeed;
        }
    }

    /** Bonus raccoglibili: gli stessi sei del gioco originale. */
    public enum PowerKind { STAR, GRENADE, HELMET, SHOVEL, TANK, CLOCK }

    // ── Misure e costanti di gioco ───────────────────────────────────────────
    public static final int TILE = 16;
    public static final int CELL = 8;
    public static final int CELLS = BattleCityMaps.TILES * 2;
    public static final int FIELD = BattleCityMaps.TILES * TILE;
    public static final int TANK = 16;
    public static final int BULLET = 4;

    public static final double PLAYER_SPEED = 52;
    public static final double PLAYER_BULLET_SPEED = 170;
    public static final int PLAYER_LIVES = 3;
    /** Nemici da eliminare per completare un livello. */
    public static final int ENEMIES_PER_LEVEL = 20;
    /** Nemici contemporaneamente in campo. */
    public static final int MAX_ENEMIES_ON_FIELD = 4;
    /** Colpi per vincere il duello. */
    public static final int DUEL_HITS = 3;

    private static final double SPAWN_INTERVAL = 2.5;   // fra due comparse di nemici
    private static final double SPAWN_SHIELD = 1.5;     // invulnerabilità dopo la comparsa
    private static final double RESPAWN_DELAY = 2.0;    // attesa prima di rimettere in campo un giocatore
    private static final double HELMET_TIME = 10;
    private static final double SHOVEL_TIME = 15;
    private static final double CLOCK_TIME = 8;
    private static final double ICE_SLIDE = 0.45;       // quota di movimento che resta sul ghiaccio
    /** Un power-up ogni quanti nemici abbattuti. */
    private static final int POWERUP_EVERY = 5;

    // ── Entità ───────────────────────────────────────────────────────────────

    /** Tank in campo (giocatore o nemico). */
    public static final class Tank {
        /** Identità stabile: serve al frontend per seguire lo stesso tank fra due snapshot. */
        public final int id;
        public final boolean player;
        /** username del giocatore, null per i nemici. */
        public final String owner;
        public final EnemyKind kind;   // null per i giocatori
        public double x, y;
        public Dir dir = Dir.UP;
        public boolean moving;
        public int hp;
        /** Livello del cannone (stelle raccolte): 1 = base, 2 = proiettile veloce, 3+ = sfonda l'acciaio. */
        public int power = 1;
        public int lives;
        public double shieldUntil;
        public double reloadAt;
        public boolean alive = true;
        public double respawnAt;
        public int score;
        /** IA: quando ridecidere la direzione. */
        double thinkAt;
        double stuckFor;

        Tank(int id, boolean player, String owner, EnemyKind kind, double x, double y, int lives) {
            this.id = id;
            this.player = player;
            this.owner = owner;
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.lives = lives;
            this.hp = kind != null ? kind.hp : 1;
        }

        public double speed() {
            return player ? PLAYER_SPEED : kind.speed;
        }

        public double bulletSpeed() {
            if (!player) return kind.bulletSpeed;
            return power >= 2 ? 240 : PLAYER_BULLET_SPEED;
        }

        /** Proiettili contemporaneamente in volo consentiti a questo tank. */
        public int maxBullets() {
            return player && power >= 3 ? 2 : 1;
        }
    }

    public static final class Bullet {
        public final int id;
        public double x, y;
        public final Dir dir;
        public final double speed;
        public final boolean fromPlayer;
        public final String owner;
        /** Potenza del cannone che l'ha sparato: da 3 sfonda l'acciaio. */
        public final int power;
        public boolean alive = true;

        Bullet(int id, double x, double y, Dir dir, double speed, boolean fromPlayer, String owner, int power) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.dir = dir;
            this.speed = speed;
            this.fromPlayer = fromPlayer;
            this.owner = owner;
            this.power = power;
        }
    }

    public static final class PowerUp {
        public final PowerKind kind;
        public final double x, y;
        public boolean taken;

        PowerUp(PowerKind kind, double x, double y) {
            this.kind = kind;
            this.x = x;
            this.y = y;
        }
    }

    /** Evento effimero per il frontend (esplosioni, bonus raccolti): non fa parte dello stato. */
    public record Event(String type, int x, int y, String by) {}

    // ── Stato ────────────────────────────────────────────────────────────────
    private final Mode mode;
    private final int level;
    private final Random rnd;
    private final Cell[][] grid = new Cell[CELLS][CELLS];
    private final List<Tank> tanks = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private final List<String> players;

    private double now;
    private int nextId = 1;
    private double nextSpawnAt = 1.0;
    private int spawnIndex;
    private int enemiesLeft;      // nemici ancora da mandare in campo + quelli in campo
    private int enemiesKilled;
    private int killsSincePowerUp;
    private double freezeUntil;   // orologio: nemici fermi
    private double shovelUntil;   // pala: base in acciaio
    private boolean gridDirty = true;
    private Status status = Status.PLAYING;
    private String winner;
    /** Posizione dell'aquila in celle (angolo alto-sinistra del 2×2). */
    private final int baseCol = CELLS / 2 - 1;
    private final int baseRow = CELLS - 2;

    public BattleCityState(Mode mode, int level, List<String> players) {
        this(mode, level, players, new Random());
    }

    /** Costruttore con Random iniettabile: rende i test deterministici. */
    public BattleCityState(Mode mode, int level, List<String> players, Random rnd) {
        this.mode = mode;
        this.level = Math.max(1, level);
        this.rnd = rnd;
        this.players = List.copyOf(players);
        loadMap(BattleCityMaps.level(this.level));
        if (mode == Mode.COOP) {
            placeBase();
            enemiesLeft = ENEMIES_PER_LEVEL;
        }
        spawnPlayers();
    }

    // ── Costruzione del campo ────────────────────────────────────────────────

    private void loadMap(String[] map) {
        for (int r = 0; r < CELLS; r++) {
            for (int c = 0; c < CELLS; c++) grid[r][c] = Cell.EMPTY;
        }
        for (int tr = 0; tr < BattleCityMaps.TILES && tr < map.length; tr++) {
            String row = map[tr];
            for (int tc = 0; tc < BattleCityMaps.TILES && tc < row.length(); tc++) {
                Cell cell = switch (row.charAt(tc)) {
                    case 'B' -> Cell.BRICK;
                    case 'S' -> Cell.STEEL;
                    case 'W' -> Cell.WATER;
                    case 'T' -> Cell.TREES;
                    case 'I' -> Cell.ICE;
                    default -> Cell.EMPTY;
                };
                if (cell == Cell.EMPTY) continue;
                // Un tile della mappa = 2×2 celle da 8px
                for (int dr = 0; dr < 2; dr++) {
                    for (int dc = 0; dc < 2; dc++) grid[tr * 2 + dr][tc * 2 + dc] = cell;
                }
            }
        }
    }

    /** Aquila al centro in basso, con la corona di mattoni che la protegge. */
    private void placeBase() {
        for (int dr = 0; dr < 2; dr++) {
            for (int dc = 0; dc < 2; dc++) grid[baseRow + dr][baseCol + dc] = Cell.BASE;
        }
        surroundBase(Cell.BRICK);
    }

    /** Sostituisce la corona attorno alla base (mattoni, o acciaio con la pala). */
    private void surroundBase(Cell material) {
        for (int r = baseRow - 1; r <= baseRow + 1; r++) {
            for (int c = baseCol - 1; c <= baseCol + 2; c++) {
                if (!inGrid(r, c)) continue;
                if (grid[r][c] == Cell.BASE) continue;
                boolean ring = r == baseRow - 1 || c == baseCol - 1 || c == baseCol + 2;
                if (ring) grid[r][c] = material;
            }
        }
        gridDirty = true;
    }

    /** Posizioni di partenza dei giocatori: ai lati della base in co-op, angoli opposti nel duello. */
    private void spawnPlayers() {
        int[][] spots = mode == Mode.COOP
                ? new int[][] { {4 * TILE, FIELD - TILE}, {8 * TILE, FIELD - TILE} }
                : new int[][] { {TILE, FIELD - 2 * TILE}, {FIELD - 2 * TILE, TILE} };
        for (int i = 0; i < players.size(); i++) {
            int[] s = spots[i % spots.length];
            Tank t = new Tank(nextId++, true, players.get(i), null, s[0], s[1], mode == Mode.COOP ? PLAYER_LIVES : 1);
            t.dir = mode == Mode.DUEL && i == 1 ? Dir.DOWN : Dir.UP;
            t.shieldUntil = SPAWN_SHIELD;
            tanks.add(t);
        }
    }

    // ── Input dei giocatori ──────────────────────────────────────────────────

    /** Direzione premuta dal giocatore; {@code dir} null = fermo. */
    public synchronized void input(String username, Dir dir, boolean moving) {
        Tank t = playerTank(username);
        if (t == null || !t.alive || status != Status.PLAYING) return;
        if (dir != null) t.dir = dir;
        t.moving = moving && dir != null;
    }

    /** Sparo del giocatore (rispetta ricarica e proiettili massimi). */
    public synchronized void shoot(String username) {
        Tank t = playerTank(username);
        if (t == null || !t.alive || status != Status.PLAYING) return;
        fire(t);
    }

    private void fire(Tank t) {
        if (now < t.reloadAt) return;
        long inFlight = bullets.stream().filter(b -> b.alive && b.owner != null && b.owner.equals(ownerKey(t))).count();
        if (inFlight >= t.maxBullets()) return;
        double cx = t.x + TANK / 2.0, cy = t.y + TANK / 2.0;
        double bx = cx - BULLET / 2.0, by = cy - BULLET / 2.0;
        switch (t.dir) {
            case UP -> by = t.y - BULLET;
            case DOWN -> by = t.y + TANK;
            case LEFT -> bx = t.x - BULLET;
            case RIGHT -> bx = t.x + TANK;
        }
        bullets.add(new Bullet(nextId++, bx, by, t.dir, t.bulletSpeed(), t.player, ownerKey(t), t.power));
        t.reloadAt = now + (t.player ? 0.35 : 0.8);
    }

    /** Chiave per legare i proiettili a chi li ha sparati (username o identità del nemico). */
    private String ownerKey(Tank t) {
        return t.player ? t.owner : "enemy#" + System.identityHashCode(t);
    }

    // ── Ciclo di gioco ───────────────────────────────────────────────────────

    /**
     * Avanza la simulazione di {@code dt} secondi.
     *
     * @return true se la griglia dei muri è cambiata (il chiamante trasmette lo snapshot completo)
     */
    public synchronized boolean tick(double dt) {
        if (status != Status.PLAYING) return false;
        now += dt;

        moveTanks(dt);
        moveBullets(dt);
        collectPowerUps();
        if (mode == Mode.COOP) {
            if (now >= shovelUntil && shovelUntil > 0) {
                surroundBase(Cell.BRICK);
                shovelUntil = 0;
            }
            spawnEnemies();
        }
        respawnPlayers();
        checkEnd();

        boolean dirty = gridDirty;
        gridDirty = false;
        return dirty;
    }

    private void moveTanks(double dt) {
        boolean frozen = now < freezeUntil;
        for (Tank t : tanks) {
            if (!t.alive) continue;
            if (!t.player) {
                if (frozen) continue;
                think(t, dt);
            }
            if (!t.moving) continue;

            double step = t.speed() * dt;
            if (onIce(t)) step *= 1 + ICE_SLIDE; // sul ghiaccio si scivola oltre
            double nx = t.x, ny = t.y;
            switch (t.dir) {
                case UP -> ny -= step;
                case DOWN -> ny += step;
                case LEFT -> nx -= step;
                case RIGHT -> nx += step;
            }
            // Allineamento a mezzo tile sull'asse perpendicolare: senza questo i corridoi da
            // 16px diventano impossibili da imboccare.
            if (t.dir == Dir.UP || t.dir == Dir.DOWN) nx = snap(nx);
            else ny = snap(ny);

            if (canStand(nx, ny, t)) {
                t.x = nx;
                t.y = ny;
                t.stuckFor = 0;
            } else {
                t.stuckFor += dt;
            }
        }
    }

    /** IA nemica: punta alla base (o al giocatore vicino), con svolte casuali e sblocco se incastrata. */
    private void think(Tank t, double dt) {
        if (t.stuckFor > 0.25) {
            t.dir = Dir.values()[rnd.nextInt(4)];
            t.moving = true;
            t.stuckFor = 0;
            t.thinkAt = now + 0.3 + rnd.nextDouble() * 0.5;
            return;
        }
        if (now >= t.thinkAt) {
            t.thinkAt = now + 0.3 + rnd.nextDouble() * 0.9;
            t.moving = true;
            Tank prey = nearestPlayer(t);
            boolean chasePlayer = prey != null && dist(t, prey) < 4 * TILE && rnd.nextDouble() < 0.6;
            double tx = chasePlayer ? prey.x : baseCol * CELL;
            double ty = chasePlayer ? prey.y : baseRow * CELL;
            if (rnd.nextDouble() < 0.65) {
                // Verso l'obiettivo: scegli l'asse con la distanza maggiore
                if (Math.abs(tx - t.x) > Math.abs(ty - t.y)) t.dir = tx > t.x ? Dir.RIGHT : Dir.LEFT;
                else t.dir = ty > t.y ? Dir.DOWN : Dir.UP;
            } else {
                t.dir = Dir.values()[rnd.nextInt(4)];
            }
        }
        // Fuoco: cadenza irregolare, e sempre se ha un giocatore in linea davanti
        if (now >= t.reloadAt && (rnd.nextDouble() < dt * 1.4 || inLineOfFire(t))) fire(t);
    }

    private boolean inLineOfFire(Tank t) {
        for (Tank p : tanks) {
            if (!p.player || !p.alive) continue;
            boolean sameCol = Math.abs(p.x - t.x) < TANK;
            boolean sameRow = Math.abs(p.y - t.y) < TANK;
            if (sameCol && ((t.dir == Dir.DOWN && p.y > t.y) || (t.dir == Dir.UP && p.y < t.y))) return true;
            if (sameRow && ((t.dir == Dir.RIGHT && p.x > t.x) || (t.dir == Dir.LEFT && p.x < t.x))) return true;
        }
        return false;
    }

    private void moveBullets(double dt) {
        for (Bullet b : bullets) {
            if (!b.alive) continue;
            double remaining = b.speed * dt;
            // Passi da massimo mezza cella: nessun proiettile attraversa un muro senza vederlo
            while (remaining > 0 && b.alive) {
                double step = Math.min(remaining, CELL / 2.0);
                remaining -= step;
                switch (b.dir) {
                    case UP -> b.y -= step;
                    case DOWN -> b.y += step;
                    case LEFT -> b.x -= step;
                    case RIGHT -> b.x += step;
                }
                resolveBullet(b);
            }
        }
        bullets.removeIf(b -> !b.alive);
    }

    private void resolveBullet(Bullet b) {
        // Fuori campo
        if (b.x < 0 || b.y < 0 || b.x + BULLET > FIELD || b.y + BULLET > FIELD) {
            b.alive = false;
            addEvent("bullet:wall", b.x, b.y, null);
            return;
        }
        // Proiettili contrapposti si annullano
        for (Bullet o : bullets) {
            if (o == b || !o.alive || o.fromPlayer == b.fromPlayer) continue;
            if (overlap(b.x, b.y, BULLET, BULLET, o.x, o.y, BULLET, BULLET)) {
                b.alive = false;
                o.alive = false;
                return;
            }
        }
        // Tank
        for (Tank t : tanks) {
            if (!t.alive || !overlap(b.x, b.y, BULLET, BULLET, t.x, t.y, TANK, TANK)) continue;
            if (b.owner != null && b.owner.equals(ownerKey(t))) continue;   // non ci si spara addosso
            if (!b.fromPlayer && !t.player) continue;                        // i nemici non si feriscono tra loro
            if (b.fromPlayer && t.player && mode == Mode.COOP) continue;     // fuoco amico disattivato in co-op
            b.alive = false;
            hit(t, b);
            return;
        }
        // Muri
        int r = (int) (b.y + BULLET / 2.0) / CELL;
        int c = (int) (b.x + BULLET / 2.0) / CELL;
        if (!inGrid(r, c)) return;
        Cell cell = grid[r][c];
        if (!cell.blocksBullet()) return;
        b.alive = false;
        if (cell == Cell.BASE) {
            if (mode == Mode.COOP) {
                addEvent("base:destroyed", b.x, b.y, null);
                status = Status.LOST;
            }
            return;
        }
        if (cell == Cell.BRICK) {
            breakBrick(r, c, b.dir);
            addEvent("bullet:brick", b.x, b.y, null);
        } else if (cell == Cell.STEEL) {
            if (b.power >= 3) {
                grid[r][c] = Cell.EMPTY;
                gridDirty = true;
                addEvent("bullet:steel", b.x, b.y, null);
            } else {
                addEvent("bullet:wall", b.x, b.y, null);
            }
        }
    }

    /**
     * Rompe i mattoni: la cella colpita più la vicina sulla trasversale, così il varco è largo
     * quanto il tank (come nell'originale, dove un colpo apre mezzo tile su tutta la larghezza).
     */
    private void breakBrick(int r, int c, Dir dir) {
        grid[r][c] = Cell.EMPTY;
        int r2 = r, c2 = c;
        if (dir == Dir.UP || dir == Dir.DOWN) c2 = c % 2 == 0 ? c + 1 : c - 1;
        else r2 = r % 2 == 0 ? r + 1 : r - 1;
        if (inGrid(r2, c2) && grid[r2][c2] == Cell.BRICK) grid[r2][c2] = Cell.EMPTY;
        gridDirty = true;
    }

    private void hit(Tank t, Bullet b) {
        if (now < t.shieldUntil) {
            addEvent("hit:shield", t.x, t.y, t.owner);
            return;
        }
        t.hp--;
        if (t.hp > 0) {
            addEvent("hit:armor", t.x, t.y, t.owner);
            return;
        }
        t.alive = false;
        addEvent("explosion", t.x, t.y, t.owner);

        if (t.player) {
            if (mode == Mode.DUEL) {
                Tank shooter = playerTank(b.owner);
                if (shooter != null) shooter.score++;
                t.respawnAt = now + RESPAWN_DELAY;
            } else {
                t.lives--;
                if (t.lives > 0) t.respawnAt = now + RESPAWN_DELAY;
            }
            return;
        }
        // Nemico abbattuto: punti a chi ha sparato, e ogni tanto lascia un bonus
        enemiesKilled++;
        Tank shooter = playerTank(b.owner);
        if (shooter != null) shooter.score += 100;
        if (++killsSincePowerUp >= POWERUP_EVERY) {
            killsSincePowerUp = 0;
            dropPowerUp();
        }
    }

    private void dropPowerUp() {
        for (int attempt = 0; attempt < 60; attempt++) {
            int tc = rnd.nextInt(BattleCityMaps.TILES);
            int tr = rnd.nextInt(BattleCityMaps.TILES - 2); // non sopra la base
            double x = tc * TILE, y = tr * TILE;
            if (canStand(x, y, null)) {
                PowerKind kind = PowerKind.values()[rnd.nextInt(PowerKind.values().length)];
                powerUps.add(new PowerUp(kind, x, y));
                addEvent("powerup:spawn", x, y, kind.name());
                return;
            }
        }
    }

    private void collectPowerUps() {
        for (PowerUp p : powerUps) {
            if (p.taken) continue;
            for (Tank t : tanks) {
                if (!t.player || !t.alive) continue;
                if (!overlap(p.x, p.y, TILE, TILE, t.x, t.y, TANK, TANK)) continue;
                p.taken = true;
                applyPowerUp(p.kind, t);
                addEvent("powerup:take", p.x, p.y, t.owner);
                break;
            }
        }
        powerUps.removeIf(p -> p.taken);
    }

    private void applyPowerUp(PowerKind kind, Tank taker) {
        switch (kind) {
            case STAR -> taker.power = Math.min(3, taker.power + 1);
            case TANK -> taker.lives++;
            case HELMET -> taker.shieldUntil = now + HELMET_TIME;
            case CLOCK -> freezeUntil = now + CLOCK_TIME;
            case SHOVEL -> {
                shovelUntil = now + SHOVEL_TIME;
                surroundBase(Cell.STEEL);
            }
            case GRENADE -> {
                for (Tank t : tanks) {
                    if (t.player || !t.alive) continue;
                    t.alive = false;
                    t.hp = 0;
                    enemiesKilled++;
                    taker.score += 100;
                    addEvent("explosion", t.x, t.y, null);
                }
            }
        }
    }

    private void spawnEnemies() {
        long onField = tanks.stream().filter(t -> !t.player && t.alive).count();
        int sent = (int) tanks.stream().filter(t -> !t.player).count();
        if (sent >= ENEMIES_PER_LEVEL || onField >= MAX_ENEMIES_ON_FIELD || now < nextSpawnAt) return;

        // Tre punti di comparsa in alto, a rotazione
        int[] cols = {0, BattleCityMaps.TILES / 2, BattleCityMaps.TILES - 1};
        int col = cols[spawnIndex++ % cols.length];
        double x = col * TILE, y = 0;
        if (!canStand(x, y, null)) return; // punto occupato: riprova al prossimo giro

        EnemyKind kind = pickEnemyKind(sent);
        Tank e = new Tank(nextId++, false, null, kind, x, y, 1);
        e.dir = Dir.DOWN;
        e.moving = true;
        e.shieldUntil = now + SPAWN_SHIELD;
        e.thinkAt = now + 0.5;
        tanks.add(e);
        addEvent("enemy:spawn", x, y, kind.name());
        nextSpawnAt = now + SPAWN_INTERVAL;
    }

    /** Composizione dell'ondata: più si avanza nel livello (e nei livelli) più tank duri. */
    private EnemyKind pickEnemyKind(int alreadySent) {
        double hard = Math.min(0.6, 0.12 * level + alreadySent * 0.02);
        double p = rnd.nextDouble();
        if (p < hard * 0.4) return EnemyKind.ARMOR;
        if (p < hard * 0.7) return EnemyKind.POWER;
        if (p < hard) return EnemyKind.FAST;
        return EnemyKind.BASIC;
    }

    private void respawnPlayers() {
        for (Tank t : tanks) {
            if (t.player && !t.alive && t.respawnAt > 0 && now >= t.respawnAt
                    && (mode == Mode.DUEL || t.lives > 0)) {
                int idx = players.indexOf(t.owner);
                int[][] spots = mode == Mode.COOP
                        ? new int[][] { {4 * TILE, FIELD - TILE}, {8 * TILE, FIELD - TILE} }
                        : new int[][] { {TILE, FIELD - 2 * TILE}, {FIELD - 2 * TILE, TILE} };
                int[] s = spots[Math.max(0, idx) % spots.length];
                t.x = s[0];
                t.y = s[1];
                t.dir = mode == Mode.DUEL && idx == 1 ? Dir.DOWN : Dir.UP;
                t.alive = true;
                t.hp = 1;
                t.moving = false;
                t.respawnAt = 0;
                t.shieldUntil = now + SPAWN_SHIELD;
                // In co-op il cannone potenziato si perde alla morte, come nell'originale
                if (mode == Mode.COOP) t.power = 1;
                addEvent("player:respawn", t.x, t.y, t.owner);
            }
        }
    }

    private void checkEnd() {
        if (mode == Mode.DUEL) {
            for (Tank t : tanks) {
                if (t.player && t.score >= DUEL_HITS) {
                    status = Status.WON;
                    winner = t.owner;
                    return;
                }
            }
            return;
        }
        if (enemiesKilled >= ENEMIES_PER_LEVEL) {
            status = Status.WON;
            return;
        }
        boolean anyPlayerLeft = tanks.stream().anyMatch(t -> t.player && (t.alive || t.lives > 0));
        if (!anyPlayerLeft) status = Status.LOST;
    }

    // ── Collisioni e utilità ─────────────────────────────────────────────────

    /** Il tank può stare con l'angolo alto-sinistra in (x,y)? (muri, bordi e altri tank) */
    private boolean canStand(double x, double y, Tank self) {
        if (x < 0 || y < 0 || x + TANK > FIELD || y + TANK > FIELD) return false;
        int c0 = (int) Math.floor(x / CELL), c1 = (int) Math.floor((x + TANK - 1) / CELL);
        int r0 = (int) Math.floor(y / CELL), r1 = (int) Math.floor((y + TANK - 1) / CELL);
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                if (!inGrid(r, c) || grid[r][c].blocksTank()) return false;
            }
        }
        for (Tank o : tanks) {
            if (o == self || !o.alive) continue;
            if (overlap(x, y, TANK, TANK, o.x, o.y, TANK, TANK)) return false;
        }
        return true;
    }

    private boolean onIce(Tank t) {
        int r = (int) ((t.y + TANK / 2.0) / CELL), c = (int) ((t.x + TANK / 2.0) / CELL);
        return inGrid(r, c) && grid[r][c] == Cell.ICE;
    }

    private static double snap(double v) {
        return Math.round(v / CELL) * (double) CELL;
    }

    private static boolean inGrid(int r, int c) {
        return r >= 0 && r < CELLS && c >= 0 && c < CELLS;
    }

    private static boolean overlap(double ax, double ay, double aw, double ah,
                                   double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private Tank playerTank(String username) {
        if (username == null) return null;
        for (Tank t : tanks) if (t.player && username.equals(t.owner)) return t;
        return null;
    }

    private Tank nearestPlayer(Tank from) {
        Tank best = null;
        double bestD = Double.MAX_VALUE;
        for (Tank t : tanks) {
            if (!t.player || !t.alive) continue;
            double d = dist(from, t);
            if (d < bestD) {
                bestD = d;
                best = t;
            }
        }
        return best;
    }

    private static double dist(Tank a, Tank b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }

    private void addEvent(String type, double x, double y, String by) {
        if (events.size() < 40) events.add(new Event(type, (int) Math.round(x), (int) Math.round(y), by));
    }

    // ── Lettura dello stato (snapshot) ───────────────────────────────────────

    /** Eventi effimeri accumulati dall'ultima chiamata (esplosioni, bonus). */
    public synchronized List<Event> drainEvents() {
        List<Event> out = List.copyOf(events);
        events.clear();
        return out;
    }

    /** Griglia come righe di caratteri (stesso alfabeto delle mappe, {@code E} = aquila). */
    public synchronized List<String> gridRows() {
        List<String> rows = new ArrayList<>(CELLS);
        for (int r = 0; r < CELLS; r++) {
            StringBuilder sb = new StringBuilder(CELLS);
            for (int c = 0; c < CELLS; c++) {
                sb.append(switch (grid[r][c]) {
                    case BRICK -> 'B';
                    case STEEL -> 'S';
                    case WATER -> 'W';
                    case TREES -> 'T';
                    case ICE -> 'I';
                    case BASE -> 'E';
                    case EMPTY -> '.';
                });
            }
            rows.add(sb.toString());
        }
        return rows;
    }

    public synchronized List<Map<String, Object>> tankViews() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Tank t : tanks) {
            if (!t.alive) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.id);
            m.put("x", round1(t.x));
            m.put("y", round1(t.y));
            m.put("dir", t.dir.name());
            m.put("player", t.player);
            if (t.player) {
                m.put("owner", t.owner);
                m.put("power", t.power);
            } else {
                m.put("kind", t.kind.name());
                m.put("hp", t.hp);
            }
            if (now < t.shieldUntil) m.put("shield", true);
            out.add(m);
        }
        return out;
    }

    public synchronized List<Map<String, Object>> bulletViews() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Bullet b : bullets) {
            if (!b.alive) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.id);
            m.put("x", round1(b.x));
            m.put("y", round1(b.y));
            m.put("dir", b.dir.name());
            m.put("player", b.fromPlayer);
            out.add(m);
        }
        return out;
    }

    public synchronized List<Map<String, Object>> powerUpViews() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (PowerUp p : powerUps) {
            if (p.taken) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kind", p.kind.name());
            m.put("x", round1(p.x));
            m.put("y", round1(p.y));
            out.add(m);
        }
        return out;
    }

    /** Vite, punti e stato del cannone per giocatore. */
    public synchronized Map<String, Map<String, Object>> playerStats() {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (Tank t : tanks) {
            if (!t.player) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("lives", Math.max(0, t.lives));
            m.put("score", t.score);
            m.put("power", t.power);
            m.put("alive", t.alive);
            out.put(t.owner, m);
        }
        return out;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    public Mode mode() { return mode; }
    public int level() { return level; }
    public synchronized Status status() { return status; }
    public synchronized String winner() { return winner; }
    public synchronized int enemiesRemaining() { return Math.max(0, ENEMIES_PER_LEVEL - enemiesKilled); }
    public synchronized int enemiesKilled() { return enemiesKilled; }
    public synchronized boolean frozen() { return now < freezeUntil; }
    public synchronized boolean shovelActive() { return shovelUntil > 0 && now < shovelUntil; }
    public List<String> players() { return players; }
    /** Solo per i test: la cella della griglia dei muri. */
    synchronized Cell cellAt(int row, int col) { return inGrid(row, col) ? grid[row][col] : null; }
    /** Solo per i test: forza una cella (per costruire scenari senza dipendere dalla mappa). */
    synchronized void setCellForTest(int row, int col, Cell cell) {
        if (inGrid(row, col)) {
            grid[row][col] = cell;
            gridDirty = true;
        }
    }
    /** Solo per i test: tank presenti (anche non vivi). */
    synchronized List<Tank> tanksForTest() { return List.copyOf(tanks); }
    synchronized List<Bullet> bulletsForTest() { return List.copyOf(bullets); }
    synchronized void addPowerUpForTest(PowerKind kind, double x, double y) { powerUps.add(new PowerUp(kind, x, y)); }
}
