package it.digitaliasistemi.minigames.game.hangman;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HangmanStateTest {

    @Test
    void winByGuessingAllLetters() {
        var s = new HangmanState("cane");
        assertEquals("____", s.masked());
        for (char c : "cane".toCharArray()) s.guess(c);
        assertEquals(HangmanState.Status.WON, s.status());
        assertEquals("cane", s.masked());
    }

    @Test
    void loseAfterMaxWrong() {
        var s = new HangmanState("cane");
        for (char c : "xyzbqw".toCharArray()) s.guess(c); // 6 errori, nessuno in "cane"
        assertEquals(HangmanState.Status.LOST, s.status());
        assertEquals(6, s.wrongCount());
        assertEquals("cane", s.masked()); // parola rivelata alla sconfitta
    }

    @Test
    void duplicateAndInvalidIgnored() {
        var s = new HangmanState("cane");
        assertTrue(s.guess('c'));
        assertFalse(s.guess('c')); // duplicato
        assertFalse(s.guess('1')); // non lettera
        assertFalse(s.guess('C')); // stessa lettera, case diverso
        assertEquals(0, s.wrongCount());
    }

    @Test
    void noChangesAfterGameOver() {
        var s = new HangmanState("ab");
        s.guess('a');
        s.guess('b');
        assertEquals(HangmanState.Status.WON, s.status());
        assertFalse(s.guess('c')); // ignorato dopo fine partita
    }

    @Test
    void repeatedLettersRevealAllOccurrences() {
        var s = new HangmanState("gatto");
        s.guess('t');
        assertEquals("__tt_", s.masked());
    }
}
