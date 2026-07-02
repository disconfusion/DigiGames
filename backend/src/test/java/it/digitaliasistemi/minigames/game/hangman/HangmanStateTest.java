package it.digitaliasistemi.minigames.game.hangman;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    // --- guessWord: tentativo dell'intera parola --------------------------------

    @Test
    void correctWordGuessWinsAndRevealsWord() {
        var s = new HangmanState("cane", List.of("alice"));
        assertTrue(s.guessWord("CANE", "alice")); // case/trim-insensitive
        assertEquals(HangmanState.Status.WON, s.status());
        assertEquals("cane", s.masked());
        assertEquals("alice", s.winner());
    }

    @Test
    void wrongWordGuessEliminatesLonerPlayerAndLoses() {
        var s = new HangmanState("cane", List.of("alice"));
        assertTrue(s.guessWord("topo", "alice"));
        assertTrue(s.isEliminated("alice"));
        assertEquals(HangmanState.Status.LOST, s.status());
        assertNull(s.winner());
    }

    @Test
    void wrongWordGuessEliminatesOnlyGuesserWhileOthersContinue() {
        var s = new HangmanState("cane", List.of("alice", "bob"));
        assertTrue(s.guessWord("topo", "alice")); // alice fuori, bob continua
        assertTrue(s.isEliminated("alice"));
        assertFalse(s.isEliminated("bob"));
        assertEquals(HangmanState.Status.PLAYING, s.status());
        assertEquals("bob", s.currentTurn()); // turno passato a bob
    }

    @Test
    void eliminatedPlayerCannotGuessLetterOrWord() {
        var s = new HangmanState("cane", List.of("alice", "bob"));
        s.guessWord("topo", "alice"); // alice eliminata
        assertFalse(s.guess('c', "alice"));
        assertFalse(s.guessWord("cane", "alice"));
    }

    @Test
    void wordGuessAllowedOutOfTurn() {
        var s = new HangmanState("cane", List.of("alice", "bob"));
        // turno di alice, ma bob può comunque tentare la parola
        assertEquals("alice", s.currentTurn());
        assertTrue(s.guessWord("cane", "bob"));
        assertEquals(HangmanState.Status.WON, s.status());
        assertEquals("bob", s.winner());
    }

    @Test
    void allPlayersEliminatedLoses() {
        var s = new HangmanState("cane", List.of("alice", "bob"));
        s.guessWord("topo", "alice");
        s.guessWord("topo", "bob");
        assertEquals(HangmanState.Status.LOST, s.status());
    }

    @Test
    void wordGuessIgnoredAfterGameOver() {
        var s = new HangmanState("ab", List.of("alice"));
        s.guess('a', "alice");
        s.guess('b', "alice");
        assertEquals(HangmanState.Status.WON, s.status());
        assertFalse(s.guessWord("ab", "alice"));
    }

    @Test
    void emptyOrNullWordGuessIgnored() {
        var s = new HangmanState("cane", List.of("alice"));
        assertFalse(s.guessWord("", "alice"));
        assertFalse(s.guessWord("   ", "alice"));
        assertFalse(s.guessWord(null, "alice"));
        assertEquals(HangmanState.Status.PLAYING, s.status());
    }
}
