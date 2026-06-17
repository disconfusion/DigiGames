package it.digitaliasistemi.minigames.game.dama;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamaStateTest {

    private long countColor(DamaState s, String color) {
        return s.board().stream()
                .flatMap(List::stream)
                .filter(c -> c != null && color.equals(c.get("color")))
                .count();
    }

    @Test
    void initialSetupHasTwelveEach() {
        var s = new DamaState("w", "b");
        assertEquals(12, countColor(s, "W"));
        assertEquals(12, countColor(s, "B"));
        assertEquals("w", s.currentTurn()); // il bianco inizia
    }

    @Test
    void whiteMovesFirstOutOfTurnRejected() {
        var s = new DamaState("w", "b");
        assertFalse(s.move("b", 2, 1, 3, 0)); // tocca al bianco
    }

    @Test
    void legalForwardMoveAccepted() {
        var s = new DamaState("w", "b");
        assertTrue(s.move("w", 5, 0, 4, 1)); // pedina bianca avanza in diagonale
        Map<String, Object> moved = s.board().get(4).get(1);
        assertNotNull(moved);
        assertEquals("W", moved.get("color"));
        assertNull(s.board().get(5).get(0)); // casa di partenza ora vuota
        assertEquals("b", s.currentTurn()); // turno passato al nero
    }

    @Test
    void cannotMoveOntoLightSquareOrNonDiagonal() {
        var s = new DamaState("w", "b");
        assertFalse(s.move("w", 5, 0, 4, 0)); // non diagonale / casa chiara
    }

    @Test
    void cannotMoveOpponentPiece() {
        var s = new DamaState("w", "b");
        assertFalse(s.move("w", 2, 1, 3, 0)); // (2,1) è del nero
    }
}
