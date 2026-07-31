package it.digitaliasistemi.minigames.words;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordDifficultyTest {

    @Test
    void lengthMapsToDifficulty() {
        assertEquals(WordDifficulty.FACILE, WordDifficulty.ofLength(4));
        assertEquals(WordDifficulty.FACILE, WordDifficulty.ofLength(6));
        assertEquals(WordDifficulty.MEDIA, WordDifficulty.ofLength(7));
        assertEquals(WordDifficulty.MEDIA, WordDifficulty.ofLength(9));
        assertEquals(WordDifficulty.DIFFICILE, WordDifficulty.ofLength(10));
        assertEquals(WordDifficulty.DIFFICILE, WordDifficulty.ofLength(18), "oltre la fascia resta difficile");
        assertEquals(WordDifficulty.FACILE, WordDifficulty.ofLength(2), "sotto la fascia resta facile");
    }

    @Test
    void randomLengthStaysInsideBand() {
        Random rnd = new Random(7);
        for (WordDifficulty d : WordDifficulty.values()) {
            for (int i = 0; i < 200; i++) {
                int len = d.randomLength(rnd);
                assertTrue(len >= d.minLength() && len <= d.maxLength(),
                        d.key() + " ha chiesto una lunghezza fuori fascia: " + len);
            }
        }
    }

    @Test
    void fromKeyAcceptsKnownKeysOnly() {
        assertEquals(WordDifficulty.FACILE, WordDifficulty.fromKey("facile"));
        assertEquals(WordDifficulty.MEDIA, WordDifficulty.fromKey(" MEDIA "));
        assertEquals(WordDifficulty.DIFFICILE, WordDifficulty.fromKey("difficile"));
        assertNull(WordDifficulty.fromKey("random"), "random = scelta dal server");
        assertNull(WordDifficulty.fromKey(null));
        assertNull(WordDifficulty.fromKey("impossibile"));
    }

    @Test
    void matchesFollowsBand() {
        assertTrue(WordDifficulty.FACILE.matches("cane"));
        assertTrue(WordDifficulty.MEDIA.matches("computer"));
        assertTrue(WordDifficulty.DIFFICILE.matches("compilatore"));
        assertTrue(!WordDifficulty.FACILE.matches("compilatore"));
    }
}
