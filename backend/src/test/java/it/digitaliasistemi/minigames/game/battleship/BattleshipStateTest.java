package it.digitaliasistemi.minigames.game.battleship;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleshipStateTest {

    private static final String P1 = "alice@x.it";
    private static final String P2 = "bob@x.it";

    /** Flotta valida lungo le colonne 0..4, navi orizzontali in righe distinte. */
    private static List<int[]> validFleet() {
        List<int[]> ships = new ArrayList<>();
        ships.add(new int[]{0, 0, 5, 1});
        ships.add(new int[]{1, 0, 4, 1});
        ships.add(new int[]{2, 0, 3, 1});
        ships.add(new int[]{3, 0, 3, 1});
        ships.add(new int[]{4, 0, 2, 1});
        return ships;
    }

    private BattleshipState started() {
        return new BattleshipState(P1, P2);
    }

    // ---- Piazzamento ----

    @Test
    void validFleetIsAccepted() {
        var s = started();
        assertTrue(s.placeFleet(P1, validFleet()));
        assertTrue(s.hasFleet(P1));
    }

    @Test
    void overlappingFleetRejected() {
        var s = started();
        List<int[]> ships = new ArrayList<>();
        ships.add(new int[]{0, 0, 5, 1});
        ships.add(new int[]{0, 0, 4, 1}); // si sovrappone con la prima
        ships.add(new int[]{2, 0, 3, 1});
        ships.add(new int[]{3, 0, 3, 1});
        ships.add(new int[]{4, 0, 2, 1});
        assertFalse(s.placeFleet(P1, ships));
        assertFalse(s.hasFleet(P1));
    }

    @Test
    void outOfBoundsFleetRejected() {
        var s = started();
        List<int[]> ships = new ArrayList<>();
        ships.add(new int[]{0, 6, 5, 1}); // colonna 6..10 -> esce dal bordo (max indice 9)
        ships.add(new int[]{1, 0, 4, 1});
        ships.add(new int[]{2, 0, 3, 1});
        ships.add(new int[]{3, 0, 3, 1});
        ships.add(new int[]{4, 0, 2, 1});
        assertFalse(s.placeFleet(P1, ships));
    }

    @Test
    void fleetMustMatchExactLengths() {
        var s = started();
        // Tutte lunghe 3: non corrisponde a [5,4,3,3,2]
        List<int[]> ships = new ArrayList<>();
        for (int r = 0; r < 5; r++) ships.add(new int[]{r, 0, 3, 1});
        assertFalse(s.placeFleet(P1, ships));

        // Numero di navi sbagliato (4 invece di 5)
        List<int[]> few = new ArrayList<>(validFleet());
        few.remove(0);
        assertFalse(s.placeFleet(P1, few));
    }

    @Test
    void fleetExactlyEquals54332() {
        // Verifica esplicita che la costante FLEET sia [5,4,3,3,2].
        assertArrayEquals(new int[]{5, 4, 3, 3, 2}, BattleshipState.FLEET);
    }

    @Test
    void randomizeProducesValidFleet() {
        var s = started();
        assertTrue(s.randomize(P1));
        assertTrue(s.hasFleet(P1));
        // La board propria deve contenere esattamente 5+4+3+3+2 = 17 celle nave.
        String[][] own = s.viewBoardOwn(P1);
        int shipCells = 0;
        for (String[] row : own) for (String cell : row) if ("S".equals(cell)) shipCells++;
        assertEquals(17, shipCells);
    }

    // ---- Transizione di fase ----

    @Test
    void readyOnlyWithValidFleet() {
        var s = started();
        assertFalse(s.ready(P1)); // nessuna flotta
        s.placeFleet(P1, validFleet());
        assertTrue(s.ready(P1));
        assertEquals(BattleshipState.Phase.PLACEMENT, s.phase()); // l'avversario non è pronto
    }

    @Test
    void bothReadyStartsBattleWithPlayer1Turn() {
        var s = started();
        s.placeFleet(P1, validFleet());
        s.placeFleet(P2, validFleet());
        s.ready(P1);
        s.ready(P2);
        assertEquals(BattleshipState.Phase.BATTLE, s.phase());
        assertEquals(P1, s.currentTurn());
    }

    private BattleshipState inBattle() {
        var s = started();
        s.placeFleet(P1, validFleet());
        s.placeFleet(P2, validFleet());
        s.ready(P1);
        s.ready(P2);
        return s;
    }

    // ---- Fuoco ----

    @Test
    void fireHitAndMiss() {
        var s = inBattle();
        // P2 ha una nave 5 in riga 0 col 0..4: (0,0) è hit. (9,9) è acqua: miss.
        assertEquals(BattleshipState.FireResult.HIT, s.fire(P1, 0, 0));
        // Ora tocca a P2; P2 manca in (9,9) (acqua per P1).
        assertEquals(BattleshipState.FireResult.MISS, s.fire(P2, 9, 9));
        // Vista nemica di P1: (0,0) = X colpito.
        assertEquals("X", s.viewBoardEnemy(P1)[0][0]);
        // Vista propria di P2: (0,0) = X (sua nave colpita).
        assertEquals("X", s.viewBoardOwn(P2)[0][0]);
        // Vista propria di P1: (9,9) = O (miss avversario su di lui).
        assertEquals("O", s.viewBoardOwn(P1)[9][9]);
    }

    @Test
    void turnAlwaysPassesEvenOnHit() {
        var s = inBattle();
        assertEquals(P1, s.currentTurn());
        s.fire(P1, 0, 0); // hit
        assertEquals(P2, s.currentTurn()); // turno passato nonostante l'hit
    }

    @Test
    void sinkingAShip() {
        var s = inBattle();
        // Affonda la nave da 2 di P2 in riga 4 col 0..1 (P1 spara, P2 spara altrove tra un colpo e l'altro).
        assertEquals(BattleshipState.FireResult.HIT, s.fire(P1, 4, 0));
        s.fire(P2, 9, 9); // miss innocuo
        assertEquals(BattleshipState.FireResult.SUNK, s.fire(P1, 4, 1));
    }

    @Test
    void fireOutOfTurnRejected() {
        var s = inBattle();
        // È il turno di P1; P2 non può sparare.
        assertEquals(BattleshipState.FireResult.INVALID, s.fire(P2, 0, 0));
    }

    @Test
    void fireOnAlreadyHitCellRejected() {
        var s = inBattle();
        s.fire(P1, 0, 0);      // P1 colpisce
        s.fire(P2, 9, 9);      // P2 manca
        // Ora di nuovo P1: ripetere (0,0) è illegale.
        assertEquals(BattleshipState.FireResult.INVALID, s.fire(P1, 0, 0));
    }

    @Test
    void winWhenAllEnemyShipsSunk() {
        var s = inBattle();
        // Tutte le 17 celle nave di P2 (righe 0..4, colonne secondo validFleet).
        int[][] cells = {
            {0, 0}, {0, 1}, {0, 2}, {0, 3}, {0, 4}, // 5
            {1, 0}, {1, 1}, {1, 2}, {1, 3},         // 4
            {2, 0}, {2, 1}, {2, 2},                 // 3
            {3, 0}, {3, 1}, {3, 2},                 // 3
            {4, 0}, {4, 1}                          // 2
        };
        BattleshipState.FireResult last = null;
        for (int i = 0; i < cells.length; i++) {
            last = s.fire(P1, cells[i][0], cells[i][1]);
            if (i < cells.length - 1) {
                // P2 spara in acqua (righe 8/9, sempre dentro i bordi) per restituire il turno a P1.
                int wr = 8 + (i / 10); // riga 8 per i<10, riga 9 per i>=10
                int wc = i % 10;       // colonna 0..9, mai ripetuta nella stessa riga
                assertEquals(BattleshipState.FireResult.MISS, s.fire(P2, wr, wc));
            }
        }
        assertEquals(BattleshipState.FireResult.WIN, last);
        assertEquals(BattleshipState.Status.WON, s.status());
        assertEquals(P1, s.winner());
        // Dopo la vittoria nessuno può più sparare.
        assertEquals(BattleshipState.FireResult.INVALID, s.fire(P2, 0, 0));
        assertNull(s.currentTurn());
    }

    @Test
    void cannotFireDuringPlacement() {
        var s = started();
        s.placeFleet(P1, validFleet());
        // Fase ancora PLACEMENT: niente fuoco.
        assertEquals(BattleshipState.FireResult.INVALID, s.fire(P1, 0, 0));
    }

    @Test
    void enemyBoardNeverRevealsUnhitShips() {
        var s = inBattle();
        // Senza aver sparato, la vista nemica di P1 è tutta "?" (mai navi avversarie).
        String[][] enemy = s.viewBoardEnemy(P1);
        for (String[] row : enemy) {
            for (String cell : row) {
                assertEquals("?", cell);
                assertNotEquals("S", cell);
            }
        }
    }
}
