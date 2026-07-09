package it.digitaliasistemi.minigames.game.tris;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrisStateTest {

    @Test
    void xStartsAndAlternates() {
        var s = new TrisState("ann", "bob");
        assertEquals("ann", s.currentTurn());
        assertTrue(s.place("ann", 0));
        assertEquals("bob", s.currentTurn());
    }

    @Test
    void rejectsMoveOutOfTurn() {
        var s = new TrisState("ann", "bob");
        assertFalse(s.place("bob", 0)); // tocca a X (ann)
        assertEquals(TrisState.Status.PLAYING, s.status());
    }

    @Test
    void rejectsOccupiedCell() {
        var s = new TrisState("ann", "bob");
        assertTrue(s.place("ann", 4));
        assertFalse(s.place("bob", 4));
    }

    @Test
    void winOnRow() {
        var s = new TrisState("ann", "bob");
        s.place("ann", 0); // X
        s.place("bob", 3); // O
        s.place("ann", 1); // X
        s.place("bob", 4); // O
        assertTrue(s.place("ann", 2)); // X completa riga 0-1-2
        assertEquals(TrisState.Status.WON, s.status());
        assertEquals("ann", s.winner());
    }

    @Test
    void drawWhenFullNoWinner() {
        var s = new TrisState("ann", "bob");
        // X O X / X O O / O X X  -> nessuna linea, pieno
        int[] order = {0, 1, 2, 4, 3, 5, 7, 6, 8};
        for (int pos : order) s.place(s.currentTurn(), pos);
        assertEquals(TrisState.Status.DRAW, s.status());
        assertNull(s.winner());
    }

    @Test
    void noMovesAfterGameOver() {
        var s = new TrisState("ann", "bob");
        s.place("ann", 0);
        s.place("bob", 3);
        s.place("ann", 1);
        s.place("bob", 4);
        s.place("ann", 2); // ann vince
        assertFalse(s.place("bob", 5));
    }

    // --- Modalità sparizione ---------------------------------------------------

    @Test
    void vanishModeRemovesOwnOldestMarkBeyondThree() {
        var s = new TrisState("ann", "bob", true);
        assertTrue(s.vanish());
        s.place("ann", 0); // X (celle X: 0)
        s.place("bob", 8); // O
        s.place("ann", 1); // X (celle X: 0,1)
        s.place("bob", 7); // O
        s.place("ann", 5); // X (celle X: 0,1,5 — nessuna linea)
        assertEquals("X", s.board()[0][0]); // cella 0 ancora X
        s.place("bob", 3); // O
        assertEquals(0, s.vanishNext()); // il prossimo piazzamento di X farà sparire la cella 0
        assertTrue(s.place("ann", 6)); // 4° segno di X → sparisce il più vecchio (cella 0)
        assertNull(s.board()[0][0]); // cella 0 ora vuota
        assertEquals("X", s.board()[2][0]); // cella 6 = riga2 col0, ora X
        assertEquals(TrisState.Status.PLAYING, s.status());
    }

    @Test
    void vanishNextMinusOneInClassicMode() {
        var s = new TrisState("ann", "bob"); // classico
        assertFalse(s.vanish());
        s.place("ann", 0);
        assertEquals(-1, s.vanishNext());
    }

    @Test
    void vanishNextMinusOneBeforeThreeMarks() {
        var s = new TrisState("ann", "bob", true);
        s.place("ann", 0); // X ha 1 solo segno
        s.place("bob", 8);
        assertEquals(-1, s.vanishNext()); // turno di X ma <3 segni: niente sparizione imminente
    }
}
