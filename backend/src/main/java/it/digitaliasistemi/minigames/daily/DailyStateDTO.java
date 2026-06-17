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

    // Stato per-utente
    boolean letterUsed,
    boolean wordAttemptUsed,
    boolean won,
    boolean eliminated
) {}
