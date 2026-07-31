package it.digitaliasistemi.minigames.game.hangman;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

/** Dizionario parole (solo a-z, niente accenti/spazi) per l'impiccato. */
public final class HangmanWords {

    private static final SecureRandom RNG = new SecureRandom();

    private static final List<String> WORDS = List.of(
            // generali
            "montagna", "albero", "finestra", "tavolo", "bicicletta", "ombrello",
            "giardino", "telefono", "cioccolato", "biblioteca", "calendario", "tramonto",
            "cane", "gatto", "elefante", "farfalla", "girasole", "cascata",
            // ufficio / colleghi
            "collega", "ufficio", "riunione", "scrivania", "stampante", "caffeina",
            // web / programmazione
            "computer", "tastiera", "programma", "internet", "browser", "server",
            "javascript", "variabile", "funzione", "database", "algoritmo", "linguaggio",
            "sviluppo", "codice", "memoria", "processore", "compilatore", "framework",
            "frontend", "backend", "quarkus", "svelte", "websocket"
    );

    private HangmanWords() {}

    public static String random() {
        return WORDS.get(RNG.nextInt(WORDS.size()));
    }

    /** Parola casuale diversa da {@code previous} (evita la stessa parola due volte di fila nella stessa stanza). */
    public static String randomExcluding(String previous) {
        if (previous == null || WORDS.size() <= 1) return random();
        String w;
        do {
            w = WORDS.get(RNG.nextInt(WORDS.size()));
        } while (w.equals(previous));
        return w;
    }

    public static String daily(LocalDate date) {
        int idx = (int) Math.abs(date.toEpochDay() % WORDS.size());
        return WORDS.get(idx);
    }

    /**
     * Parola locale di lunghezza compresa fra {@code min} e {@code max} (estremi inclusi),
     * diversa da {@code exclude}. Usata come ripiego quando il dizionario online non risponde:
     * la difficoltà richiesta viene rispettata anche offline. Se nessuna parola locale rientra
     * nella fascia, ne torna una qualsiasi.
     */
    public static String randomInRange(int min, int max, String exclude) {
        List<String> pool = WORDS.stream()
                .filter(w -> w.length() >= min && w.length() <= max)
                .filter(w -> !w.equals(exclude))
                .toList();
        if (pool.isEmpty()) return randomExcluding(exclude);
        return pool.get(RNG.nextInt(pool.size()));
    }

    /** Tutte le parole locali (sola lettura): usata dai test e per le statistiche del dizionario. */
    public static List<String> all() {
        return WORDS;
    }
}
