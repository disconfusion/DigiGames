package it.digitaliasistemi.minigames.daily;

import it.digitaliasistemi.minigames.domain.DailyAttempt;
import it.digitaliasistemi.minigames.domain.DailyWordPick;
import it.digitaliasistemi.minigames.domain.DailyWordState;
import it.digitaliasistemi.minigames.game.hangman.HangmanState;
import it.digitaliasistemi.minigames.leaderboard.LeaderboardService;
import it.digitaliasistemi.minigames.util.RequestContexts;
import it.digitaliasistemi.minigames.words.WordDifficulty;
import it.digitaliasistemi.minigames.words.WordService;
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

    @Inject WordService words;
    @Inject DailyWordPickStore picks;

    /**
     * Giornate per cui il download della parola è già in corso: la prima richiesta lo avvia, le
     * altre vedono solo lo stato "in caricamento" (nessuna fetch duplicata).
     */
    private final Set<LocalDate> fetching = Collections.synchronizedSet(new HashSet<>());

    /**
     * Parola della giornata: quella dell'admin se impostata, altrimenti quella estratta dal
     * dizionario. {@code word} è null finché il download non è finito ({@code loading}).
     */
    private record DailyWord(String word, WordDifficulty difficulty, boolean fromDictionary, boolean custom) {
        boolean loading() { return word == null; }
        String wordOrEmpty() { return word == null ? "" : word; }
    }

    @Transactional
    public DailyStateDTO getState(LocalDate date, String username) {
        DailyWordState shared = getOrCreateShared(date);
        DailyAttempt attempt = getOrCreateAttempt(date, username);
        return toDTO(shared, attempt, wordFor(shared, date));
    }

    @Transactional
    public DailyStateDTO guessLetter(LocalDate date, String username, char letter) {
        DailyWordState shared = getOrCreateShared(date);
        DailyAttempt attempt = getOrCreateAttempt(date, username);
        DailyWord daily = wordFor(shared, date);

        // Parola non ancora pronta (download in corso): nessun tentativo viene consumato
        if (daily.loading()) return toDTO(shared, attempt, daily);

        String word = daily.word();

        // Slot già usato, eliminato o partita terminata
        if (attempt.letterUsed || attempt.eliminated || !"PLAYING".equals(shared.status)) {
            return toDTO(shared, attempt, daily);
        }

        attempt.letterUsed = true;
        attempt.myLetter = String.valueOf(letter);

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

        return toDTO(shared, attempt, daily);
    }

    @Transactional
    public DailyStateDTO guessWord(LocalDate date, String username, String guessedWord) {
        DailyWordState shared = getOrCreateShared(date);
        DailyAttempt attempt = getOrCreateAttempt(date, username);
        DailyWord daily = wordFor(shared, date);

        // Parola non ancora pronta (download in corso): nessun tentativo viene consumato
        if (daily.loading()) return toDTO(shared, attempt, daily);

        String word = daily.word();

        if (attempt.wordAttemptUsed || attempt.eliminated || !"PLAYING".equals(shared.status)) {
            return toDTO(shared, attempt, daily);
        }

        attempt.wordAttemptUsed = true;
        attempt.wordGuess = guessedWord.trim();

        if (word.equalsIgnoreCase(guessedWord.trim())) {
            shared.status = "WON";
            shared.winner = username;
            attempt.won = true;
            leaderboard.record(username, "daily", "WIN");
        } else {
            // Tentativo parola sbagliato → eliminato dall'impiccato del giorno (solo oggi)
            attempt.eliminated = true;
        }

        return toDTO(shared, attempt, daily);
    }

    // -------------------------------------------------------------------------

    /** Imposta parola custom e resetta la partita di oggi. */
    @Transactional
    public void adminSetWord(LocalDate date, String word) {
        DailyWordState shared = getOrCreateShared(date);
        shared.customWord = word;
        shared.revealedLetters = "";
        shared.wrongLetters = "";
        shared.status = "PLAYING";
        shared.winner = null;
        DailyAttempt.delete("date", date);
    }

    /** Imposta (o rimuove, se vuoto) il callout dell'admin per la giornata. */
    @Transactional
    public void adminSetCallout(LocalDate date, String message) {
        DailyWordState shared = getOrCreateShared(date);
        shared.calloutMessage = (message != null && !message.isBlank()) ? message.trim() : null;
    }

    /**
     * Elimina stato condiviso, tentativi e parola estratta di oggi: alla richiesta successiva
     * si riparte con una nuova estrazione (nuova difficoltà, nuova parola dal dizionario).
     */
    @Transactional
    public void adminResetDaily(LocalDate date) {
        DailyWordState existing = DailyWordState.findByDate(date);
        if (existing != null) existing.delete();
        DailyAttempt.delete("date", date);
        DailyWordPick.delete("date", date);
    }

    /**
     * Parola della giornata. L'admin ha la precedenza; altrimenti si usa quella estratta e salvata
     * per quella data. Se non c'è ancora, il download parte (una volta sola) e la partita resta
     * "in caricamento" finché la parola non è pronta: la difficoltà del giorno è estratta a caso
     * al momento del download.
     */
    private DailyWord wordFor(DailyWordState shared, LocalDate date) {
        if (shared.customWord != null && !shared.customWord.isBlank()) {
            String w = shared.customWord.trim().toLowerCase();
            return new DailyWord(w, WordDifficulty.ofLength(w.length()), false, true);
        }
        DailyWordPick pick = picks.find(date);
        if (pick != null) {
            WordDifficulty d = WordDifficulty.fromKey(pick.difficulty);
            return new DailyWord(pick.word, d != null ? d : WordDifficulty.ofLength(pick.word.length()),
                    pick.fromDictionary, false);
        }
        startFetch(date);
        return new DailyWord(null, null, false, false);
    }

    /** Avvia il download della parola del giorno, se non è già in corso. */
    private void startFetch(LocalDate date) {
        if (!fetching.add(date)) return;
        WordDifficulty diff = words.randomDifficulty();
        words.pickAsync("it", diff, null, pick -> {
            try {
                RequestContexts.run(() -> picks.save(date, pick));
            } finally {
                fetching.remove(date);
            }
        });
    }

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

    private DailyStateDTO toDTO(DailyWordState shared, DailyAttempt attempt, DailyWord daily) {
        Set<Character> revealed = parseLetters(shared.revealedLetters);
        Set<Character> wrong = parseLetters(shared.wrongLetters);

        String word = daily.wordOrEmpty();
        StringBuilder masked = new StringBuilder();
        for (char c : word.toCharArray()) {
            masked.append(revealed.contains(c) ? c : '_');
        }

        String revealedWord = daily.loading() || "PLAYING".equals(shared.status) ? null : word;

        return new DailyStateDTO(
            masked.toString(),
            wrong.stream().map(String::valueOf).sorted().toList(),
            revealed.stream().map(String::valueOf).sorted().toList(),
            wrong.size(),
            HangmanState.MAX_WRONG,
            shared.status,
            shared.winner,
            revealedWord,
            daily.custom(),
            shared.calloutMessage,
            daily.difficulty() != null ? daily.difficulty().key() : null,
            daily.difficulty() != null ? daily.difficulty().label() : null,
            daily.fromDictionary(),
            daily.loading(),
            attempt.letterUsed,
            attempt.wordAttemptUsed,
            attempt.won,
            attempt.eliminated,
            attempt.myLetter,
            attempt.wordGuess
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
