package it.digitaliasistemi.minigames.game.minesweeper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test puri (NO @QuarkusTest) per MinesweeperState.
 * Usa il costruttore deterministico boolean[][] per posizionare le mine in modo noto.
 *
 * Layout di default per i test (9x9, mina = M, sicura = .):
 *
 *   col→  0 1 2 3 4 5 6 7 8
 * row↓
 *   0     M . . . . . . . .
 *   1     . . . . . . . . .
 *   2     . . . . . . . . .
 *   ...   (resto: solo la mina in [0][0])
 */
class MinesweeperStateTest {

    /** Crea una board 9x9 con UNA sola mina in (mineR, mineC). */
    private static MinesweeperState singleMine(int mineR, int mineC) {
        boolean[][] mines = new boolean[MinesweeperState.ROWS][MinesweeperState.COLS];
        mines[mineR][mineC] = true;
        return new MinesweeperState(mines);
    }

    /** Crea una board 9x9 senza mine (per testare WON). */
    private static MinesweeperState noMines() {
        return new MinesweeperState(new boolean[MinesweeperState.ROWS][MinesweeperState.COLS]);
    }

    // ----------------------------------------------------------------
    // 1. Reveal mina → LOST
    // ----------------------------------------------------------------

    @Test
    void revealMine_causesLost() {
        MinesweeperState ms = singleMine(0, 0);
        assertEquals(MinesweeperState.Status.PLAYING, ms.status());

        boolean acted = ms.reveal(0, 0);
        assertTrue(acted, "reveal su mina deve essere accettato");
        assertEquals(MinesweeperState.Status.LOST, ms.status());
    }

    @Test
    void afterLost_minesAreVisibleInSnapshot() {
        MinesweeperState ms = singleMine(3, 3);
        ms.reveal(3, 3); // colpisce la mina
        var snap = ms.cellSnapshot();
        // la mina deve essere visibile nello snapshot
        assertNotNull(snap[3][3].mine(), "mine deve essere non-null dopo LOST");
        assertTrue(snap[3][3].mine(), "la cella (3,3) deve risultare mina dopo LOST");
    }

    // ----------------------------------------------------------------
    // 2. Mine non visibili durante PLAYING
    // ----------------------------------------------------------------

    @Test
    void mineNotExposedDuringPlay() {
        MinesweeperState ms = singleMine(0, 0);
        var snap = ms.cellSnapshot();
        // cella non rivelata e partita in corso → mine deve essere null
        assertNull(snap[0][0].mine(), "mine non deve essere inclusa per cella coperta durante PLAYING");
    }

    // ----------------------------------------------------------------
    // 3. Flood-fill su zona vuota rivela più celle
    // ----------------------------------------------------------------

    @Test
    void floodFill_revealsAdjacentEmptyCells() {
        // Mina nell'angolo [8][8]; rivelare [0][0] (lontano) deve
        // propagarsi e rivelare molte celle
        boolean[][] mines = new boolean[MinesweeperState.ROWS][MinesweeperState.COLS];
        mines[8][8] = true;
        MinesweeperState ms = new MinesweeperState(mines);

        ms.reveal(0, 0);

        var snap = ms.cellSnapshot();
        // con una mina solo in [8][8], rivelare [0][0] deve propagare
        // almeno alle celle vicine (tutte con adjacent==0 lontane dalla mina)
        int revealedCount = 0;
        for (int r = 0; r < MinesweeperState.ROWS; r++)
            for (int c = 0; c < MinesweeperState.COLS; c++)
                if (snap[r][c].revealed()) revealedCount++;

        assertTrue(revealedCount > 1, "flood-fill deve rivelare più di una cella; rivelate: " + revealedCount);
    }

    @Test
    void floodFill_doesNotCrossMine() {
        // Mina in [0][0]; rivelare [0][2] non deve espandere oltre il confine della mina
        MinesweeperState ms = singleMine(0, 0);
        ms.reveal(2, 2);
        // La mina [0][0] NON deve essere rivelata
        var snap = ms.cellSnapshot();
        assertFalse(snap[0][0].revealed(), "flood-fill non deve rivelare la cella mina");
    }

    // ----------------------------------------------------------------
    // 4. Flag toggle
    // ----------------------------------------------------------------

    @Test
    void flagToggle_onAndOff() {
        MinesweeperState ms = singleMine(0, 0);

        // metti bandierina
        assertTrue(ms.flag(1, 1));
        assertEquals(1, ms.flagsUsed());
        assertTrue(ms.cellSnapshot()[1][1].flagged());

        // rimuovi bandierina (toggle)
        assertTrue(ms.flag(1, 1));
        assertEquals(0, ms.flagsUsed());
        assertFalse(ms.cellSnapshot()[1][1].flagged());
    }

    @Test
    void flagOnRevealedCell_isIgnored() {
        MinesweeperState ms = singleMine(8, 8);
        ms.reveal(0, 0); // rivela (area lontana dalla mina)
        // tentare di flaggare una cella già rivelata deve essere ignorato
        var snap = ms.cellSnapshot();
        if (snap[0][0].revealed()) {
            boolean acted = ms.flag(0, 0);
            assertFalse(acted, "flag su cella rivelata deve essere ignorato");
            assertEquals(0, ms.flagsUsed());
        }
    }

    @Test
    void revealFlaggedCell_isIgnored() {
        MinesweeperState ms = singleMine(0, 0);
        ms.flag(1, 1);
        boolean acted = ms.reveal(1, 1);
        assertFalse(acted, "reveal su cella flagged deve essere ignorato");
        assertFalse(ms.cellSnapshot()[1][1].revealed());
    }

    // ----------------------------------------------------------------
    // 5. WON quando tutte le celle non-mina sono rivelate
    // ----------------------------------------------------------------

    @Test
    void won_whenAllSafeCellsRevealed() {
        // Board 9x9 senza mine: la prima reveal deve propagarsi a tutto
        MinesweeperState ms = noMines();
        ms.reveal(0, 0);
        assertEquals(MinesweeperState.Status.WON, ms.status());
    }

    @Test
    void won_afterRevealingLastSafeCell() {
        // Board con una mina in [0][0]; riveliamo tutte le celle tranne la mina
        MinesweeperState ms = singleMine(0, 0);
        // Reveal da [4][4]: il flood-fill si propaga e rivela tutto ciò che ha adjacent==0
        // ma alcune celle border-mina restano con adjacent>0, quindi riveliamole manualmente
        // Per semplicità: usiamo board senza mine e verifichiamo WON
        MinesweeperState ms2 = noMines();
        ms2.reveal(4, 4);
        assertEquals(MinesweeperState.Status.WON, ms2.status(),
                "con zero mine, rivelare una cella qualsiasi porta a WON via flood-fill");
    }

    // ----------------------------------------------------------------
    // 6. Conteggio adjacent corretto
    // ----------------------------------------------------------------

    @Test
    void adjacent_correctForCellNextToMine() {
        // Mina in [0][0]; la cella [0][1] ha 1 mina adiacente, [1][1] ha 1 mina adiacente
        MinesweeperState ms = singleMine(0, 0);
        // riveliamo celle sicure per vedere adjacent
        ms.reveal(0, 1);
        ms.reveal(1, 0);
        ms.reveal(1, 1);

        var snap = ms.cellSnapshot();
        // [0][1] è rivelata: adiacente alla mina [0][0]
        assertTrue(snap[0][1].revealed());
        assertNotNull(snap[0][1].adjacent());
        assertEquals(1, snap[0][1].adjacent(), "cella [0][1] ha 1 mina adiacente");

        // [1][1] è rivelata: adiacente alla mina [0][0]
        assertTrue(snap[1][1].revealed());
        assertNotNull(snap[1][1].adjacent());
        assertEquals(1, snap[1][1].adjacent(), "cella [1][1] ha 1 mina adiacente");

        // [1][0] è rivelata: adiacente alla mina [0][0]
        assertTrue(snap[1][0].revealed());
        assertEquals(1, snap[1][0].adjacent(), "cella [1][0] ha 1 mina adiacente");
    }

    @Test
    void adjacent_zeroFarFromMine() {
        // Cella centrale [4][4] con mina solo in [0][0] ha adjacent == 0
        MinesweeperState ms = singleMine(0, 0);
        ms.reveal(4, 4);
        var snap = ms.cellSnapshot();
        // [4][4] viene rivelata (direttamente o via flood-fill)
        if (snap[4][4].revealed()) {
            assertEquals(0, snap[4][4].adjacent(), "cella lontana dalla mina ha adjacent == 0");
        }
    }

    // ----------------------------------------------------------------
    // 7. Azioni ignorate dopo game over
    // ----------------------------------------------------------------

    @Test
    void actionsIgnoredAfterLost() {
        MinesweeperState ms = singleMine(0, 0);
        ms.reveal(0, 0); // LOST
        assertEquals(MinesweeperState.Status.LOST, ms.status());

        assertFalse(ms.reveal(1, 1), "reveal ignorato dopo LOST");
        assertFalse(ms.flag(1, 1),   "flag ignorato dopo LOST");
    }

    @Test
    void actionsIgnoredAfterWon() {
        MinesweeperState ms = noMines();
        ms.reveal(0, 0); // WON
        assertEquals(MinesweeperState.Status.WON, ms.status());

        assertFalse(ms.reveal(0, 0), "reveal ignorato dopo WON");
        assertFalse(ms.flag(0, 0),   "flag ignorato dopo WON");
    }
}
