package it.digitaliasistemi.minigames.daily;

import java.util.List;

public record DailyStateDTO(
    String masked,
    List<String> wrong,
    List<String> guessed,
    int wrongCount,
    int maxWrong,
    String status,
    String word
) {}
