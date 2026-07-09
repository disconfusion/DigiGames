package it.digitaliasistemi.minigames.game.hangman;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HangmanWordsTest {

    @Test
    void randomExcludingNeverReturnsPrevious() {
        String prev = HangmanWords.random();
        // Molte iterazioni: non deve mai restituire la parola precedente.
        for (int i = 0; i < 500; i++) {
            String w = HangmanWords.randomExcluding(prev);
            assertNotEquals(prev, w);
            prev = w;
        }
    }

    @Test
    void randomExcludingNullReturnsAnyWord() {
        assertNotNull(HangmanWords.randomExcluding(null));
    }
}
