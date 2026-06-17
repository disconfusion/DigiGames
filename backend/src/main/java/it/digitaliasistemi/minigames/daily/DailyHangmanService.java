package it.digitaliasistemi.minigames.daily;

import it.digitaliasistemi.minigames.domain.DailyAttempt;
import it.digitaliasistemi.minigames.game.hangman.HangmanState;
import it.digitaliasistemi.minigames.game.hangman.HangmanWords;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class DailyHangmanService {

    @Transactional
    public DailyStateDTO getOrCreate(LocalDate date, String username) {
        String word = HangmanWords.daily(date);
        DailyAttempt attempt = DailyAttempt.findByDateAndUser(date, username);
        if (attempt == null) {
            attempt = new DailyAttempt();
            attempt.date = date;
            attempt.username = username;
            attempt.persist();
        }
        return toDTO(word, attempt);
    }

    @Transactional
    public DailyStateDTO guess(LocalDate date, String username, char letter) {
        String word = HangmanWords.daily(date);
        DailyAttempt attempt = DailyAttempt.findByDateAndUser(date, username);
        if (attempt == null) {
            attempt = new DailyAttempt();
            attempt.date = date;
            attempt.username = username;
            attempt.persist();
        }

        if (!"PLAYING".equals(attempt.status)) {
            return toDTO(word, attempt);
        }

        Set<Character> guessed = parseLetters(attempt.guessed);
        Set<Character> wrong = parseLetters(attempt.wrong);

        if (guessed.contains(letter) || wrong.contains(letter)) {
            return toDTO(word, attempt);
        }

        if (word.indexOf(letter) >= 0) {
            guessed.add(letter);
            attempt.guessed = joinLetters(guessed);
        } else {
            wrong.add(letter);
            attempt.wrong = joinLetters(wrong);
        }

        boolean won = word.chars().allMatch(c -> guessed.contains((char) c));
        if (won) {
            attempt.status = "WON";
        } else if (wrong.size() >= HangmanState.MAX_WRONG) {
            attempt.status = "LOST";
        }

        return toDTO(word, attempt);
    }

    private DailyStateDTO toDTO(String word, DailyAttempt attempt) {
        Set<Character> guessed = parseLetters(attempt.guessed);
        Set<Character> wrong = parseLetters(attempt.wrong);

        StringBuilder masked = new StringBuilder();
        for (char c : word.toCharArray()) {
            masked.append(guessed.contains(c) ? c : '_');
        }

        List<String> wrongList = wrong.stream().map(String::valueOf).sorted().toList();
        List<String> guessedList = guessed.stream().map(String::valueOf).sorted().toList();
        String revealedWord = "PLAYING".equals(attempt.status) ? null : word;

        return new DailyStateDTO(
            masked.toString(), wrongList, guessedList,
            wrong.size(), HangmanState.MAX_WRONG, attempt.status, revealedWord
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
