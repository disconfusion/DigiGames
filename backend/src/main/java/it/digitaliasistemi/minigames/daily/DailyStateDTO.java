package it.digitaliasistemi.minigames.daily;

import java.util.List;

public record DailyStateDTO(
    // Stato condiviso
    String masked,
    List<String> wrongLetters,
    List<String> revealedLetters,
    int wrongCount,
    int maxWrong,
    String status,   // PLAYING | WON | LOST
    String winner,   // username vincitore, null se ancora in gioco
    String word,     // rivelata solo quando status != PLAYING
    boolean custom,  // true se la parola è stata scelta manualmente dall'admin
    String callout,  // messaggio callout dell'admin per la giornata, null se assente

    // Parola del giorno: difficoltà estratta per la giornata e provenienza
    String difficulty,      // "facile" | "media" | "difficile" (null mentre si carica)
    String difficultyLabel, // etichetta da mostrare nel badge
    boolean dictionary,     // true se la parola arriva dal dizionario online, false = parole locali
    boolean loading,        // true finché la parola del giorno non è stata scaricata

    // Stato per-utente
    boolean letterUsed,
    boolean wordAttemptUsed,
    boolean won,
    boolean eliminated,
    String myLetter,    // la lettera giocata dall'utente (null se non ancora usata)
    String wordGuess    // la parola tentata dall'utente (null se non ancora tentata)
) {}
