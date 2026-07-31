package it.digitaliasistemi.minigames.game.battlecity;

import it.digitaliasistemi.minigames.game.battlecity.BattleCityState.Cell;
import it.digitaliasistemi.minigames.game.battlecity.BattleCityState.Dir;
import it.digitaliasistemi.minigames.game.battlecity.BattleCityState.Mode;
import it.digitaliasistemi.minigames.game.battlecity.BattleCityState.PowerKind;
import it.digitaliasistemi.minigames.game.battlecity.BattleCityState.Status;
import it.digitaliasistemi.minigames.game.battlecity.BattleCityState.Tank;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleCityStateTest {

    private static final double DT = 1.0 / 60;

    private static BattleCityState coop(String... players) {
        return new BattleCityState(Mode.COOP, 1, List.of(players), new Random(42));
    }

    private static BattleCityState duel() {
        return new BattleCityState(Mode.DUEL, 1, List.of("ann", "bob"), new Random(42));
    }

    private static void run(BattleCityState s, int ticks) {
        for (int i = 0; i < ticks; i++) s.tick(DT);
    }

    private static Tank playerTank(BattleCityState s, String who) {
        return s.tanksForTest().stream()
                .filter(t -> t.player && who.equals(t.owner)).findFirst().orElse(null);
    }

    @Test
    void baseAndItsBrickRingArePlacedInCoop() {
        var s = coop("ann");
        int row = BattleCityState.CELLS - 2, col = BattleCityState.CELLS / 2 - 1;
        assertEquals(Cell.BASE, s.cellAt(row, col), "aquila al centro in basso");
        assertEquals(Cell.BASE, s.cellAt(row + 1, col + 1));
        assertEquals(Cell.BRICK, s.cellAt(row - 1, col), "corona di mattoni sopra la base");
        assertEquals(Cell.BRICK, s.cellAt(row, col - 1), "corona di mattoni a sinistra");
    }

    @Test
    void duelHasNoBaseAndNoEnemies() {
        var s = duel();
        int row = BattleCityState.CELLS - 2, col = BattleCityState.CELLS / 2 - 1;
        assertEquals(Cell.EMPTY, s.cellAt(row, col), "nel duello non c'è l'aquila");
        run(s, 300);
        assertTrue(s.tanksForTest().stream().noneMatch(t -> !t.player), "nel duello non compaiono nemici");
    }

    @Test
    void playerMovesOnlyWhenAskedAndStaysInsideField() {
        var s = coop("ann");
        Tank t = playerTank(s, "ann");
        double y0 = t.y;
        run(s, 30);
        assertEquals(y0, t.y, 0.001, "senza input il tank resta fermo");

        s.input("ann", Dir.UP, true);
        run(s, 30);
        assertTrue(t.y < y0, "premendo su, il tank sale");

        s.input("ann", Dir.DOWN, true);
        run(s, 600);
        assertTrue(t.y + BattleCityState.TANK <= BattleCityState.FIELD, "non esce dal campo");
    }

    @Test
    void ignoresInputFromNonPlayers() {
        var s = coop("ann");
        Tank t = playerTank(s, "ann");
        double x0 = t.x, y0 = t.y;
        s.input("estraneo", Dir.UP, true);
        s.shoot("estraneo");
        run(s, 30);
        assertEquals(x0, t.x, 0.001);
        assertEquals(y0, t.y, 0.001);
    }

    @Test
    void bulletBreaksBricksAndOpensAGap() {
        var s = coop("ann");
        Tank t = playerTank(s, "ann");
        int row = BattleCityState.CELLS - 2, col = BattleCityState.CELLS / 2 - 1;
        // Tank sopra la corona di mattoni della base, con la strada libera, e spara verso il basso
        clear(s, row - 4, col - 1, row - 2, col + 2);
        t.x = col * BattleCityState.CELL;
        t.y = (row - 4) * BattleCityState.CELL;
        s.input("ann", Dir.DOWN, false);
        s.shoot("ann");
        run(s, 30);
        assertEquals(Cell.EMPTY, s.cellAt(row - 1, col), "il mattone colpito sparisce");
        assertEquals(Cell.EMPTY, s.cellAt(row - 1, col + 1), "il varco è largo mezzo tile");
    }

    @Test
    void steelResistsUntilTheCannonIsUpgraded() {
        // Livello 2 ha fortini d'acciaio: qui si testa la regola, non la mappa
        var s = coop("ann");
        Tank t = playerTank(s, "ann");
        // Acciaio davanti al tank, con il tratto in mezzo sgomberato dai muri della mappa
        int r = 10, c = 10;
        clear(s, r, c, r + 5, c + 1);
        t.x = c * BattleCityState.CELL;
        t.y = (r + 3) * BattleCityState.CELL;
        setCell(s, r, c, Cell.STEEL);
        setCell(s, r, c + 1, Cell.STEEL);

        s.input("ann", Dir.UP, false);
        s.shoot("ann");
        run(s, 40);
        assertEquals(Cell.STEEL, s.cellAt(r, c), "col cannone base l'acciaio resiste");
        assertEquals(Cell.STEEL, s.cellAt(r, c + 1), "nessuna delle due celle cede");

        // Il proiettile parte dal centro del tank, quindi colpisce la cella di destra:
        // l'acciaio, a differenza dei mattoni, cede solo nella cella centrata dal colpo.
        t.power = 3; // due stelle raccolte
        s.shoot("ann");
        run(s, 40);
        assertEquals(Cell.EMPTY, s.cellAt(r, c + 1), "col cannone potenziato l'acciaio salta");
    }

    @Test
    void waterBlocksTanksButNotBullets() {
        var s = coop("ann");
        Tank t = playerTank(s, "ann");
        int r = 12, c = 12;
        clear(s, r, c, r + 5, c + 1);
        t.x = c * BattleCityState.CELL;
        t.y = (r + 3) * BattleCityState.CELL;
        setCell(s, r, c, Cell.WATER);
        setCell(s, r, c + 1, Cell.WATER);
        setCell(s, r + 1, c, Cell.WATER);
        setCell(s, r + 1, c + 1, Cell.WATER);

        double y0 = t.y;
        s.input("ann", Dir.UP, true);
        run(s, 60);
        assertTrue(t.y >= y0 - BattleCityState.CELL, "l'acqua ferma il tank");

        s.shoot("ann");
        run(s, 10);
        assertEquals(Cell.WATER, s.cellAt(r, c), "l'acqua non si distrugge");
    }

    @Test
    void enemiesAppearOverTimeButNeverMoreThanFourAtOnce() {
        var s = coop("ann");
        for (int i = 0; i < 60 * 60; i++) {
            s.tick(DT);
            long onField = s.tanksForTest().stream().filter(t -> !t.player && t.alive).count();
            assertTrue(onField <= BattleCityState.MAX_ENEMIES_ON_FIELD,
                    "troppi nemici in campo: " + onField);
        }
        assertTrue(s.tanksForTest().stream().anyMatch(t -> !t.player), "i nemici devono comparire");
    }

    @Test
    void destroyingTheBaseLosesTheGame() {
        var s = coop("ann");
        int row = BattleCityState.CELLS - 2, col = BattleCityState.CELLS / 2 - 1;
        // Sgombra la corona e spara alla base dall'alto
        for (int c = col - 1; c <= col + 2; c++) setCell(s, row - 1, c, Cell.EMPTY);
        Tank t = playerTank(s, "ann");
        t.x = col * BattleCityState.CELL;
        t.y = (row - 4) * BattleCityState.CELL;
        s.input("ann", Dir.DOWN, false);
        s.shoot("ann");
        run(s, 60);
        assertEquals(Status.LOST, s.status(), "l'aquila colpita chiude la partita");
    }

    @Test
    void grenadeWipesEnemiesOnField() {
        var s = coop("ann");
        run(s, 60 * 12); // lascia comparire qualche nemico
        long before = s.tanksForTest().stream().filter(t -> !t.player && t.alive).count();
        assertTrue(before > 0, "servono nemici in campo per il test");

        int killedBefore = s.enemiesKilled();
        Tank t = playerTank(s, "ann");
        s.addPowerUpForTest(PowerKind.GRENADE, t.x, t.y);
        run(s, 1);
        assertEquals(killedBefore + before, s.enemiesKilled(),
                "la granata abbatte tutti i nemici che erano in campo");
    }

    @Test
    void starUpgradesCannonUpToThree() {
        var s = coop("ann");
        Tank t = playerTank(s, "ann");
        assertEquals(1, t.power);
        for (int i = 0; i < 5; i++) {
            s.addPowerUpForTest(PowerKind.STAR, t.x, t.y);
            run(s, 2);
        }
        assertEquals(3, t.power, "il cannone si ferma al terzo livello");
    }

    @Test
    void helmetMakesPlayerImmuneForAWhile() {
        var s = coop("ann");
        Tank t = playerTank(s, "ann");
        s.addPowerUpForTest(PowerKind.HELMET, t.x, t.y);
        run(s, 2);
        assertTrue(s.tankViews().stream().anyMatch(v -> Boolean.TRUE.equals(v.get("shield"))),
                "lo scudo è visibile nello snapshot");
    }

    @Test
    void shovelTurnsBaseRingIntoSteelAndBackAgain() {
        var s = coop("ann");
        int row = BattleCityState.CELLS - 2, col = BattleCityState.CELLS / 2 - 1;
        Tank t = playerTank(s, "ann");
        s.addPowerUpForTest(PowerKind.SHOVEL, t.x, t.y);
        run(s, 2);
        assertEquals(Cell.STEEL, s.cellAt(row - 1, col), "la pala mura la base in acciaio");
        assertTrue(s.shovelActive());
        run(s, (int) (60 * 16)); // oltre la durata
        assertEquals(Cell.BRICK, s.cellAt(row - 1, col), "poi torna mattone");
        assertFalse(s.shovelActive());
    }

    @Test
    void clockFreezesEnemies() {
        var s = coop("ann");
        run(s, 60 * 10);
        Tank enemy = s.tanksForTest().stream().filter(t -> !t.player && t.alive).findFirst().orElse(null);
        assertNotNull(enemy, "serve un nemico in campo");
        Tank t = playerTank(s, "ann");
        s.addPowerUpForTest(PowerKind.CLOCK, t.x, t.y);
        run(s, 2);
        assertTrue(s.frozen());
        double ex = enemy.x, ey = enemy.y;
        run(s, 60);
        assertEquals(ex, enemy.x, 0.001, "i nemici congelati non si muovono");
        assertEquals(ey, enemy.y, 0.001);
    }

    @Test
    void duelIsWonAfterThreeHits() {
        var s = duel();
        Tank a = playerTank(s, "ann");
        Tank b = playerTank(s, "bob");
        for (int i = 0; i < BattleCityState.DUEL_HITS; i++) {
            // Affianca i due tank e spara a bruciapelo
            b.x = a.x;
            b.y = a.y - BattleCityState.TANK - 2;
            b.alive = true;
            b.hp = 1;
            b.shieldUntil = 0;
            a.shieldUntil = 0;
            a.reloadAt = 0;
            s.input("ann", Dir.UP, false);
            s.shoot("ann");
            run(s, 12);
        }
        assertEquals(Status.WON, s.status());
        assertEquals("ann", s.winner());
    }

    @Test
    void coopHasNoFriendlyFire() {
        var s = coop("ann", "bob");
        Tank a = playerTank(s, "ann");
        Tank b = playerTank(s, "bob");
        b.x = a.x;
        b.y = a.y - BattleCityState.TANK - 2;
        a.shieldUntil = 0;
        b.shieldUntil = 0;
        s.input("ann", Dir.UP, false);
        s.shoot("ann");
        run(s, 12);
        assertTrue(b.alive, "in co-op non si colpisce il compagno");
    }

    @Test
    void playerLosesALifeAndComesBack() {
        var s = coop("ann");
        Tank t = playerTank(s, "ann");
        int lives0 = t.lives;
        t.shieldUntil = 0;
        // Un nemico gli spara addosso: lo si simula piazzando un nemico sopra di lui
        run(s, 60 * 6);
        Tank enemy = s.tanksForTest().stream().filter(x -> !x.player && x.alive).findFirst().orElse(null);
        assertNotNull(enemy);
        enemy.x = t.x;
        enemy.y = t.y - BattleCityState.TANK - 2;
        enemy.dir = Dir.DOWN;
        enemy.reloadAt = 0;
        t.shieldUntil = 0;
        run(s, 60 * 3);
        assertTrue(t.lives < lives0 || !t.alive, "il giocatore colpito perde una vita");
        run(s, 60 * 4);
        assertTrue(t.alive, "e poi rientra in campo");
    }

    @Test
    void levelIsWonWhenAllEnemiesAreDown() {
        var s = coop("ann");
        // Simula l'abbattimento di tutti i nemici con granate ripetute
        for (int round = 0; round < 30 && s.status() == Status.PLAYING; round++) {
            run(s, 60 * 4);
            Tank t = playerTank(s, "ann");
            s.addPowerUpForTest(PowerKind.GRENADE, t.x, t.y);
            run(s, 2);
        }
        assertEquals(Status.WON, s.status(), "abbattuti tutti i nemici il livello è vinto");
        assertEquals(0, s.enemiesRemaining());
    }

    @Test
    void gridSnapshotHasRightShapeAndAlphabet() {
        var s = coop("ann");
        List<String> rows = s.gridRows();
        assertEquals(BattleCityState.CELLS, rows.size());
        for (String row : rows) {
            assertEquals(BattleCityState.CELLS, row.length());
            assertTrue(row.matches("[.BSWTIE]+"), "caratteri inattesi nella griglia: " + row);
        }
    }

    @Test
    void everyLevelMapIsWellFormed() {
        for (int level = 1; level <= 12; level++) {
            String[] map = BattleCityMaps.level(level);
            assertEquals(BattleCityMaps.TILES, map.length, "righe della mappa " + level);
            for (String row : map) {
                assertEquals(BattleCityMaps.TILES, row.length(), "colonne della mappa " + level);
                assertTrue(row.matches("[.BSWTI]+"), "caratteri inattesi nella mappa " + level + ": " + row);
            }
        }
    }

    @Test
    void generatedLevelsAreSymmetricAndKeepBaseReachable() {
        for (int level = 7; level <= 15; level++) {
            String[] map = BattleCityMaps.level(level);
            int mid = BattleCityMaps.TILES / 2;
            for (String row : map) {
                for (int c = 0; c < BattleCityMaps.TILES / 2; c++) {
                    assertEquals(row.charAt(c), row.charAt(BattleCityMaps.TILES - 1 - c),
                            "mappa " + level + " non simmetrica");
                }
            }
            for (String row : map) {
                char mc = row.charAt(mid);
                assertTrue(mc != 'S' && mc != 'W', "corridoio centrale ostruito nella mappa " + level);
            }
        }
    }

    @Test
    void samePlayerBulletsAreLimited() {
        var s = coop("ann");
        Tank t = playerTank(s, "ann");
        s.input("ann", Dir.UP, false);
        for (int i = 0; i < 5; i++) {
            s.shoot("ann");
            s.tick(DT);
        }
        long mine = s.bulletsForTest().stream().filter(b -> b.alive && "ann".equals(b.owner)).count();
        assertTrue(mine <= 1, "col cannone base un solo proiettile in volo, trovati " + mine);
        t.power = 3;
        t.reloadAt = 0;
        s.shoot("ann");
        s.tick(DT);
        t.reloadAt = 0;
        s.shoot("ann");
        s.tick(DT);
        mine = s.bulletsForTest().stream().filter(b -> b.alive && "ann".equals(b.owner)).count();
        assertTrue(mine <= 2, "col cannone potenziato al massimo due, trovati " + mine);
    }

    @Test
    void nothingHappensAfterGameOver() {
        var s = duel();
        Tank a = playerTank(s, "ann");
        Tank b = playerTank(s, "bob");
        for (int i = 0; i < BattleCityState.DUEL_HITS; i++) {
            b.x = a.x;
            b.y = a.y - BattleCityState.TANK - 2;
            b.alive = true;
            b.hp = 1;
            b.shieldUntil = 0;
            a.reloadAt = 0;
            s.input("ann", Dir.UP, false);
            s.shoot("ann");
            run(s, 12);
        }
        assertEquals(Status.WON, s.status());
        assertFalse(s.tick(DT), "a partita finita il tick non fa nulla");
        double x0 = a.x;
        s.input("ann", Dir.RIGHT, true);
        run(s, 30);
        assertEquals(x0, a.x, 0.001, "e non si muove più nessuno");
        assertNull(playerTank(s, "carla"), "giocatori inesistenti non compaiono");
    }

    /** Scorciatoia per i test: forza una cella della griglia dei muri. */
    private static void setCell(BattleCityState s, int row, int col, Cell cell) {
        s.setCellForTest(row, col, cell);
    }

    /** Sgombera un rettangolo di celle: gli scenari non devono inciampare nei muri della mappa. */
    private static void clear(BattleCityState s, int row0, int col0, int row1, int col1) {
        for (int r = row0; r <= row1; r++) {
            for (int c = col0; c <= col1; c++) s.setCellForTest(r, c, Cell.EMPTY);
        }
    }
}
