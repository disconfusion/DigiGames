package it.digitaliasistemi.minigames.game.hangman;

import it.digitaliasistemi.minigames.words.WordDifficulty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Il ripiego locale deve rispettare la difficoltà richiesta anche senza dizionario online. */
class HangmanWordsRangeTest {

    @Test
    void everyDifficultyHasLocalWords() {
        for (WordDifficulty d : WordDifficulty.values()) {
            long n = HangmanWords.all().stream().filter(d::matches).count();
            assertTrue(n > 0, "nessuna parola locale per la difficoltà " + d.key());
        }
    }

    @Test
    void randomInRangeRespectsBand() {
        for (WordDifficulty d : WordDifficulty.values()) {
            for (int i = 0; i < 100; i++) {
                String w = HangmanWords.randomInRange(d.minLength(), d.maxLength(), null);
                assertTrue(d.matches(w), d.key() + " ha restituito \"" + w + "\" (" + w.length() + " lettere)");
            }
        }
    }

    @Test
    void randomInRangeAvoidsPreviousWord() {
        String prev = HangmanWords.randomInRange(7, 9, null);
        for (int i = 0; i < 50; i++) {
            assertNotEquals(prev, HangmanWords.randomInRange(7, 9, prev));
        }
    }

    @Test
    void unsatisfiableBandFallsBackToAnyWord() {
        String w = HangmanWords.randomInRange(30, 40, null); // nessuna parola così lunga
        assertTrue(w != null && !w.isBlank(), "deve comunque restituire una parola");
    }
}
