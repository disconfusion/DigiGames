package it.digitaliasistemi.minigames.daily;

import it.digitaliasistemi.minigames.domain.DailyAttempt;
import it.digitaliasistemi.minigames.domain.DailyWordState;
import it.digitaliasistemi.minigames.game.hangman.HangmanState;
import it.digitaliasistemi.minigames.game.hangman.HangmanWords;
import it.digitaliasistemi.minigames.leaderboard.LeaderboardService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class DailyHangmanService {

    @Inject
    LeaderboardService leaderboard;

    @Transactional
    public DailyStateDTO getState(LocalDate date, String username) {
        DailyWordState shared = getOrCreateShared(date);
        DailyAttempt attempt = getOrCreateAttempt(date, username);
        return toDTO(shared, attempt, HangmanWords.daily(date));
    }

    @Transactional
    public DailyStateDTO guessLetter(LocalDate date, String username, char letter) {
        DailyWordState shared = getOrCreateShared(date);
        DailyAttempt attempt = getOrCreateAttempt(date, username);
        String word = HangmanWords.daily(date);

        // Slot già usato, eliminato o partita terminata
        if (attempt.letterUsed || attempt.eliminated || !"PLAYING".equals(shared.status)) {
            return toDTO(shared, attempt, word);
        }

        attempt.letterUsed = true;

        Set<Character> revealed = parseLetters(shared.revealedLetters);
        Set<Character> wrong = parseLetters(shared.wrongLetters);

        // Lettera già presente nello stato condiviso — slot consumato senza effetto
        if (!revealed.contains(letter) && !wrong.contains(letter)) {
            if (word.indexOf(letter) >= 0) {
                revealed.add(letter);
                shared.revealedLetters = joinLetters(revealed);
            } else {
                wrong.add(letter);
                shared.wrongLetters = joinLetters(wrong);
            }
        }

        // Controlla fine partita
        boolean allRevealed = word.chars().allMatch(c -> revealed.contains((char) c));
        if (allRevealed) {
            shared.status = "WON";
            shared.winner = username;
            attempt.won = true;
            leaderboard.record(username, "daily", "WIN");
        } else if (wrong.size() >= HangmanState.MAX_WRONG) {
            shared.status = "LOST";
        }

        return toDTO(shared, attempt, word);
    }

    @Transactional
    public DailyStateDTO guessWord(LocalDate date, String username, String guessedWord) {
        DailyWordState shared = getOrCreateShared(date);
        DailyAttempt attempt = getOrCreateAttempt(date, username);
        String word = HangmanWords.daily(date);

        if (attempt.wordAttemptUsed || attempt.eliminated || !"PLAYING".equals(shared.status)) {
            return toDTO(shared, attempt, word);
        }

        attempt.wordAttemptUsed = true;

        if (word.equalsIgnoreCase(guessedWord.trim())) {
            shared.status = "WON";
            shared.winner = username;
            attempt.won = true;
            leaderboard.record(username, "daily", "WIN");
        } else {
            // Tentativo parola sbagliato → eliminato dall'impiccato del giorno (solo oggi)
            attempt.eliminated = true;
        }

        return toDTO(shared, attempt, word);
    }

    // -------------------------------------------------------------------------

    private DailyWordState getOrCreateShared(LocalDate date) {
        DailyWordState s = DailyWordState.findByDate(date);
        if (s == null) {
            s = new DailyWordState();
            s.date = date;
            s.persist();
        }
        return s;
    }

    private DailyAttempt getOrCreateAttempt(LocalDate date, String username) {
        DailyAttempt a = DailyAttempt.findByDateAndUser(date, username);
        if (a == null) {
            a = new DailyAttempt();
            a.date = date;
            a.username = username;
            a.persist();
        }
        return a;
    }

    private DailyStateDTO toDTO(DailyWordState shared, DailyAttempt attempt, String word) {
        Set<Character> revealed = parseLetters(shared.revealedLetters);
        Set<Character> wrong = parseLetters(shared.wrongLetters);

        StringBuilder masked = new StringBuilder();
        for (char c : word.toCharArray()) {
            masked.append(revealed.contains(c) ? c : '_');
        }

        String revealedWord = "PLAYING".equals(shared.status) ? null : word;

        return new DailyStateDTO(
            masked.toString(),
            wrong.stream().map(String::valueOf).sorted().toList(),
            revealed.stream().map(String::valueOf).sorted().toList(),
            wrong.size(),
            HangmanState.MAX_WRONG,
            shared.status,
            shared.winner,
            revealedWord,
            attempt.letterUsed,
            attempt.wordAttemptUsed,
            attempt.won,
            attempt.eliminated
        );
    }

    private Set<Character> parseLetters(String s) {
        Set<Character> set = new LinkedHashSet<>();
        if (s != null && !s.isBlank()) {
            for (String part : s.split(",")) {
                if (!part.isBlank()) set.add(part.charAt(0));
            }
        }
        return set;
    }

    private String joinLetters(Set<Character> set) {
        return set.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
