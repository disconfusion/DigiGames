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

    public static String daily(LocalDate date) {
        int idx = (int) Math.abs(date.toEpochDay() % WORDS.size());
        return WORDS.get(idx);
    }
}
