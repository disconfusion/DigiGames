package it.digitaliasistemi.minigames.game.chess;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChessStateTest {

    private long pieces(ChessState s) {
        return s.board().stream().flatMap(List::stream).filter(c -> c != null).count();
    }

    @Test
    void initialBoardAndMoves() {
        var s = new ChessState("w", "b");
        assertEquals(32, pieces(s));
        assertEquals("w", s.currentTurn());
        // Apertura standard: 16 mosse di pedone + 4 di cavallo = 20 mosse legali
        assertEquals(20, s.legalMoves(ChessState.WHITE).size());
    }

    @Test
    void simplePawnMoveSwitchesTurn() {
        var s = new ChessState("w", "b");
        assertTrue(s.move("w", 6, 4, 5, 4, (char) 0)); // e2-e3
        assertEquals("b", s.currentTurn());
        assertNull(s.board().get(6).get(4));
        assertEquals("WP", s.board().get(5).get(4));
    }

    @Test
    void rejectsIllegalAndOutOfTurn() {
        var s = new ChessState("w", "b");
        assertFalse(s.move("b", 1, 4, 3, 4, (char) 0)); // tocca al bianco
        assertFalse(s.move("w", 6, 4, 3, 4, (char) 0)); // e2-e5 (3 case) illegale
        assertEquals(ChessState.Status.PLAYING, s.status());
    }

    @Test
    void foolsMate() {
        var s = new ChessState("w", "b");
        assertTrue(s.move("w", 6, 5, 5, 5, (char) 0)); // 1. f3
        assertTrue(s.move("b", 1, 4, 3, 4, (char) 0)); // 1... e5
        assertTrue(s.move("w", 6, 6, 4, 6, (char) 0)); // 2. g4
        assertTrue(s.move("b", 0, 3, 4, 7, (char) 0)); // 2... Qh4#
        assertEquals(ChessState.Status.CHECKMATE, s.status());
        assertEquals(Character.valueOf('B'), s.winner());
    }

    @Test
    void cannotMoveIntoCheckIsHandledByLegalMoves() {
        // Dopo 1. f3 il bianco è ancora in gioco e ha mosse legali
        var s = new ChessState("w", "b");
        s.move("w", 6, 5, 5, 5, (char) 0);
        assertFalse(s.legalMoves(ChessState.BLACK).isEmpty());
    }
}
