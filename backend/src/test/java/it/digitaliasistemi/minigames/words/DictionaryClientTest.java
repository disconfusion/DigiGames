package it.digitaliasistemi.minigames.words;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryClientTest {

    @Test
    void sanitizeKeepsPlainWords() {
        assertEquals("montagna", DictionaryClient.sanitize("Montagna"));
        assertEquals("montagna", DictionaryClient.sanitize("  montagna  "));
    }

    @Test
    void sanitizeStripsAccents() {
        assertEquals("perche", DictionaryClient.sanitize("perché"));
        assertEquals("citta", DictionaryClient.sanitize("città"));
        assertEquals("uber", DictionaryClient.sanitize("über"));
    }

    @Test
    void sanitizeRejectsUnusableWords() {
        assertNull(DictionaryClient.sanitize("due parole"), "spazi non ammessi");
        assertNull(DictionaryClient.sanitize("stand-by"), "trattini non ammessi");
        assertNull(DictionaryClient.sanitize("dell'anno"), "apostrofi non ammessi");
        assertNull(DictionaryClient.sanitize("ab"), "troppo corta per l'impiccato");
        assertNull(DictionaryClient.sanitize("汉字"), "alfabeti non latini non giocabili");
        assertNull(DictionaryClient.sanitize(""));
        assertNull(DictionaryClient.sanitize(null));
    }

    @Test
    void knownLanguages() {
        assertTrue(DictionaryClient.isKnownLanguage("it"));
        assertTrue(DictionaryClient.isKnownLanguage("PT-BR"));
        assertTrue(!DictionaryClient.isKnownLanguage("klingon"));
        assertTrue(!DictionaryClient.isKnownLanguage(null));
    }

    @Test
    void unreachableDictionaryReturnsEmptyInsteadOfThrowing() {
        // Host inesistente + timeout breve: il client non deve propagare eccezioni, il chiamante
        // deve poter ripiegare sulle parole locali.
        var client = new DictionaryClient("http://localhost:1/nope", Duration.ofMillis(300));
        Optional<String> w = client.fetch("it", WordDifficulty.MEDIA);
        assertTrue(w.isEmpty(), "dizionario non raggiungibile deve dare vuoto");
    }
}
