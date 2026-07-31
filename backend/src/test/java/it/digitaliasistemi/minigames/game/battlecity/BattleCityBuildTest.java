package it.digitaliasistemi.minigames.game.battlecity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleCityBuildTest {

    private static final int N = BattleCityMaps.TILES;
    private static final int MID = N / 2;

    private static BattleCityBuild duo() {
        return new BattleCityBuild(List.of("ann", "bob"));
    }

    @Test
    void eachPlayerOwnsHalfTheField() {
        var b = duo();
        assertEquals(0, b.zoneOf("ann"));
        assertEquals(1, b.zoneOf("bob"));
        assertEquals(-1, b.zoneOf("carla"), "chi non gioca non ha zona");

        int row = 5;
        assertTrue(b.canPaint("ann", row, 1), "ann dipinge a sinistra");
        assertFalse(b.canPaint("ann", row, MID + 1), "ann non dipinge nella metà di bob");
        assertTrue(b.canPaint("bob", row, MID + 1), "bob dipinge a destra");
        assertFalse(b.canPaint("bob", row, 1), "bob non dipinge nella metà di ann");
    }

    @Test
    void middleColumnBelongsToNobody() {
        var b = duo();
        assertFalse(BattleCityBuild.isPaintable(5, MID), "la colonna centrale resta libera");
        assertFalse(b.paint("ann", 5, MID, 'S'));
        assertFalse(b.paint("bob", 5, MID, 'S'));
        assertEquals('.', b.rows()[5].charAt(MID));
    }

    @Test
    void spawnRowsAndBaseAreaAreProtected() {
        var b = duo();
        for (int r = 0; r < BattleCityBuild.RESERVED_TOP; r++) {
            assertFalse(b.paint("ann", r, 1, 'S'), "riga di comparsa " + r + " non dipingibile");
        }
        for (int r = N - BattleCityBuild.RESERVED_BOTTOM; r < N; r++) {
            assertFalse(b.paint("ann", r, 1, 'S'), "zona base (riga " + r + ") non dipingibile");
        }
    }

    @Test
    void paintingWorksAndIsIdempotent() {
        var b = duo();
        assertTrue(b.paint("ann", 4, 2, 'B'));
        assertEquals('B', b.rows()[4].charAt(2));
        assertFalse(b.paint("ann", 4, 2, 'B'), "ridipingere lo stesso tile non cambia nulla");
        assertTrue(b.paint("ann", 4, 2, '.'), "si può cancellare");
        assertEquals('.', b.rows()[4].charAt(2));
    }

    @Test
    void unknownTilesAreRejected() {
        var b = duo();
        assertFalse(b.paint("ann", 4, 2, 'X'));
        assertFalse(b.paint("ann", 4, 2, 'E'), "l'aquila la piazza il gioco, non i giocatori");
        assertEquals('.', b.rows()[4].charAt(2));
    }

    @Test
    void readyDecaysWhenYouKeepDrawing() {
        var b = duo();
        b.setReady("ann", true);
        assertTrue(b.isReady("ann"));
        b.paint("ann", 4, 2, 'B');
        assertFalse(b.isReady("ann"), "chi riprende a disegnare non è più pronto");
    }

    @Test
    void everyoneReadyNeedsAllPlayers() {
        var b = duo();
        b.setReady("ann", true);
        assertFalse(b.everyoneReady());
        b.setReady("bob", true);
        assertTrue(b.everyoneReady());
        b.setReady("bob", false);
        assertFalse(b.everyoneReady());
    }

    @Test
    void soloPlayerPaintsTheWholeField() {
        var b = new BattleCityBuild(List.of("ann"));
        assertTrue(b.canPaint("ann", 5, 1));
        assertTrue(b.canPaint("ann", 5, N - 2), "da solo si disegna anche l'altra metà");
        assertFalse(b.canPaint("ann", 5, MID), "tranne la colonna centrale");
    }

    @Test
    void toolsActOnlyOnYourOwnZone() {
        var b = duo();
        assertTrue(b.fillRandom("ann", new Random(1)));
        String[] rows = b.rows();
        for (String row : rows) {
            for (int c = MID; c < N; c++) {
                assertEquals('.', row.charAt(c), "il riempimento di ann ha invaso la metà di bob");
            }
        }
        assertTrue(b.clearZone("ann"));
        for (String row : b.rows()) {
            for (int c = 0; c < MID; c++) {
                assertEquals('.', row.charAt(c), "dopo lo svuotamento la metà di ann è vuota");
            }
        }
    }

    @Test
    void mirrorCopiesYourHalfToTheOtherSide() {
        var b = duo();
        b.paint("ann", 4, 2, 'B');
        b.paint("ann", 6, 0, 'S');
        assertTrue(b.mirror("ann"));
        String[] rows = b.rows();
        assertEquals('B', rows[4].charAt(N - 1 - 2), "il mattone è stato specchiato");
        assertEquals('S', rows[6].charAt(N - 1), "l'acciaio è stato specchiato");
    }

    @Test
    void emptyMapIsPlayable() {
        var b = duo();
        assertTrue(BattleCityBuild.isPlayable(b.rows()), "campo vuoto: si arriva alla base");
    }

    @Test
    void steelWallAcrossTheFieldMakesMapUnplayable() {
        var b = duo();
        // Muro d'acciaio su tutta la larghezza dipingibile, colonna centrale compresa? No: quella
        // resta libera, quindi per rendere la mappa impossibile serve chiuderla altrove.
        String[] rows = b.rows();
        // Costruisce a mano una mappa con una fascia d'acciaio continua (test della regola)
        char[] wall = new char[N];
        java.util.Arrays.fill(wall, 'S');
        rows[6] = new String(wall);
        assertFalse(BattleCityBuild.isPlayable(rows), "una fascia d'acciaio continua taglia il campo");
    }

    @Test
    void waterAlsoBlocksThePathButBricksDoNot() {
        var b = duo();
        String[] rows = b.rows();
        char[] water = new char[N];
        java.util.Arrays.fill(water, 'W');
        rows[6] = new String(water);
        assertFalse(BattleCityBuild.isPlayable(rows), "l'acqua non si attraversa");

        char[] bricks = new char[N];
        java.util.Arrays.fill(bricks, 'B');
        rows[6] = new String(bricks);
        assertTrue(BattleCityBuild.isPlayable(rows), "i mattoni si aprono a colpi: la mappa è giocabile");
    }

    @Test
    void sanitizeFixesAnythingComingFromOutside() {
        String[] junk = { "XXXX", null, "BBBBBBBBBBBBBBBBBBBB" };
        String[] clean = BattleCityBuild.sanitize(junk);
        assertEquals(N, clean.length);
        for (String row : clean) assertEquals(N, row.length());
        assertEquals('.', clean[0].charAt(0), "le righe di comparsa restano vuote");
        for (String row : clean) {
            for (char ch : row.toCharArray()) {
                assertTrue(BattleCityBuild.PAINTABLE.indexOf(ch) >= 0, "carattere non ammesso: " + ch);
            }
        }
    }

    @Test
    void builtMapCanBePlayed() {
        var b = duo();
        b.fillRandom("ann", new Random(3));
        b.fillRandom("bob", new Random(4));
        String[] rows = BattleCityBuild.sanitize(b.rows());
        var st = new BattleCityState(BattleCityState.Mode.COOP, 1, List.of("ann", "bob"), new Random(5), rows);
        assertTrue(st.customMap(), "la partita sa di girare su una mappa disegnata");
        // L'aquila e la sua corona ci sono comunque: le piazza il gioco, non l'editor
        int row = BattleCityState.CELLS - 2, col = BattleCityState.CELLS / 2 - 1;
        assertEquals(BattleCityState.Cell.BASE, st.cellAt(row, col));
        assertEquals(BattleCityState.Cell.BRICK, st.cellAt(row - 1, col));
        for (int i = 0; i < 120; i++) st.tick(1.0 / 60);
        assertEquals(BattleCityState.Status.PLAYING, st.status(), "la partita gira sulla mappa disegnata");
    }
}
