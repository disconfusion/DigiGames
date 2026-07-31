package it.digitaliasistemi.minigames.words;

import java.util.Random;

/**
 * Livello di difficoltà di una parola dell'impiccato, espresso come fascia di lunghezza.
 *
 * <p>Il dizionario online ({@code random-word-api}) ha un parametro {@code ?diff} basato sulla
 * frequenza Wikipedia, ma è inutilizzabile: con {@code lang=it} l'applicazione risponde 503 e
 * quando risponde impiega 10-18 s. La lunghezza (parametro {@code ?length}) è invece rapida e
 * affidabile, ed è una misura di difficoltà onesta per l'impiccato: più lettere = più tentativi.
 */
public enum WordDifficulty {

    FACILE("facile", "Facile", 4, 6),
    MEDIA("media", "Media", 7, 9),
    DIFFICILE("difficile", "Difficile", 10, 13);

    private final String key;
    private final String label;
    private final int minLength;
    private final int maxLength;

    WordDifficulty(String key, String label, int minLength, int maxLength) {
        this.key = key;
        this.label = label;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    public String key() { return key; }
    public String label() { return label; }
    public int minLength() { return minLength; }
    public int maxLength() { return maxLength; }

    /** Lunghezza a caso dentro la fascia: è quella richiesta al dizionario. */
    public int randomLength(Random rnd) {
        return minLength + rnd.nextInt(maxLength - minLength + 1);
    }

    public boolean matches(String word) {
        int n = word == null ? 0 : word.length();
        return n >= minLength && n <= maxLength;
    }

    /**
     * Difficoltà di una parola già scelta, dedotta dalla sua lunghezza: le parole più corte
     * della fascia facile restano FACILE, quelle più lunghe della difficile restano DIFFICILE.
     */
    public static WordDifficulty ofLength(int length) {
        if (length <= FACILE.maxLength) return FACILE;
        if (length <= MEDIA.maxLength) return MEDIA;
        return DIFFICILE;
    }

    /** Livello da chiave testuale ("facile"/"media"/"difficile"); null se sconosciuta o "random". */
    public static WordDifficulty fromKey(String key) {
        if (key == null) return null;
        String k = key.trim().toLowerCase();
        for (WordDifficulty d : values()) {
            if (d.key.equals(k)) return d;
        }
        return null;
    }

    public static WordDifficulty random(Random rnd) {
        return values()[rnd.nextInt(values().length)];
    }
}
