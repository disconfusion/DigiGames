package it.digitaliasistemi.minigames.game.hangman;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HangmanState {

    public enum Status { PLAYING, WON, LOST }

    /** Parti del corpo base — usato dal daily e come default senza config. */
    public static final int MAX_WRONG = HangmanConfig.BASE_PARTS;

    private static final String VOWELS = "aeiou";

    private final String word;
    private final Set<Character> guessed = new LinkedHashSet<>();
    private final Set<Character> wrong = new LinkedHashSet<>();
    private Status status = Status.PLAYING;

    // Turn tracking — empty list = nessun controllo turno (usato nei test)
    private final List<String> players;
    private int turnIndex = 0;

    // Config
    private final HangmanConfig config;
    private int vowelsCalled = 0;
    private final Map<String, Integer> lettersUsed = new HashMap<>();

    /** Costruttore per test: nessun giocatore, nessun controllo turno, regole classiche. */
    public HangmanState(String word) {
        this(word, List.of());
    }

    /** Costruttore con lista giocatori ordinata e regole classiche (default). */
    public HangmanState(String word, List<String> players) {
        this(word, players, new HangmanConfig(0, 0, List.of()));
    }

    /** Costruttore di produzione con regole personalizzate. */
    public HangmanState(String word, List<String> players, HangmanConfig config) {
        this.word = word.toLowerCase();
        this.players = List.copyOf(players);
        this.config = config;
    }

    // --- Config / limiti ----------------------------------------------------

    public int maxWrong() { return config.maxWrong(); }
    public int maxVowels() { return config.maxVowels(); }
    public int vowelsCalled() { return vowelsCalled; }
    public int lettersPerPlayer() { return config.lettersPerPlayer(); }
    public List<String> accessories() { return config.accessories(); }

    public synchronized Map<String, Integer> lettersUsed() { return new HashMap<>(lettersUsed); }

    private boolean hasVowelBudget() {
        return config.maxVowels() <= 0 || vowelsCalled < config.maxVowels();
    }

    private boolean hasLetterBudget(String player) {
        if (config.lettersPerPlayer() <= 0) return true;
        return lettersUsed.getOrDefault(player, 0) < config.lettersPerPlayer();
    }

    // --- Turni --------------------------------------------------------------

    /** Username del giocatore di turno, null se turni non attivi. */
    public synchronized String currentTurn() {
        if (players.isEmpty()) return null;
        return players.get(turnIndex % players.size());
    }

    /** True se è il turno di questo giocatore (sempre true se turni non attivi). */
    public synchronized boolean isMyTurn(String player) {
        return players.isEmpty() || player.equals(currentTurn());
    }

    /** Avanza al prossimo giocatore con budget lettere; se nessuno può giocare → LOST. */
    private void advanceTurn() {
        if (players.isEmpty()) return;
        if (config.lettersPerPlayer() <= 0) {
            turnIndex = (turnIndex + 1) % players.size();
            return;
        }
        for (int i = 1; i <= players.size(); i++) {
            int idx = (turnIndex + i) % players.size();
            if (hasLetterBudget(players.get(idx))) {
                turnIndex = idx;
                return;
            }
        }
        // Nessun giocatore ha più lettere disponibili e la parola non è completa
        if (status == Status.PLAYING) status = Status.LOST;
    }

    // --- Mosse --------------------------------------------------------------

    /**
     * Prova una lettera da parte di un giocatore specifico.
     * Ritorna false se non è il suo turno, lettera non valida/duplicata, budget esaurito,
     * limite vocali raggiunto, o partita finita.
     */
    public synchronized boolean guess(char input, String player) {
        if (!isMyTurn(player)) return false;
        char c = Character.toLowerCase(input);
        if (status != Status.PLAYING) return false;
        if (c < 'a' || c > 'z') return false;
        if (guessed.contains(c) || wrong.contains(c)) return false;
        if (!hasLetterBudget(player)) return false;
        boolean vowel = VOWELS.indexOf(c) >= 0;
        if (vowel && !hasVowelBudget()) return false;

        if (vowel) vowelsCalled++;
        if (!players.isEmpty()) lettersUsed.merge(player, 1, Integer::sum);

        applyGuess(c);
        if (status == Status.PLAYING) advanceTurn();
        return true;
    }

    /** Versione senza controllo turno/budget — per test con costruttore no-players. */
    public synchronized boolean guess(char input) {
        char c = Character.toLowerCase(input);
        if (status != Status.PLAYING) return false;
        if (c < 'a' || c > 'z') return false;
        if (guessed.contains(c) || wrong.contains(c)) return false;
        boolean vowel = VOWELS.indexOf(c) >= 0;
        if (vowel && !hasVowelBudget()) return false;
        if (vowel) vowelsCalled++;
        applyGuess(c);
        return true;
    }

    private void applyGuess(char c) {
        if (word.indexOf(c) >= 0) {
            guessed.add(c);
            if (isComplete()) status = Status.WON;
        } else {
            wrong.add(c);
            if (wrong.size() >= config.maxWrong()) status = Status.LOST;
        }
    }

    private boolean isComplete() {
        for (char c : word.toCharArray()) {
            if (Character.isLetter(c) && !guessed.contains(c)) return false;
        }
        return true;
    }

    public synchronized String masked() {
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (!Character.isLetter(c)) sb.append(c);
            else if (guessed.contains(c) || status == Status.LOST) sb.append(c);
            else sb.append('_');
        }
        return sb.toString();
    }

    public synchronized Status status() { return status; }
    public synchronized Set<Character> wrong() { return new LinkedHashSet<>(wrong); }
    public synchronized Set<Character> guessed() { return new LinkedHashSet<>(guessed); }
    public synchronized int wrongCount() { return wrong.size(); }
    public String word() { return word; }
}
