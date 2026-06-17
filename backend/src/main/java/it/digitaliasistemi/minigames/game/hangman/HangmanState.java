package it.digitaliasistemi.minigames.game.hangman;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HangmanState {

    public enum Status { PLAYING, WON, LOST }

    public static final int MAX_WRONG = 6;

    private final String word;
    private final Set<Character> guessed = new LinkedHashSet<>();
    private final Set<Character> wrong = new LinkedHashSet<>();
    private Status status = Status.PLAYING;

    // Turn tracking — empty list = nessun controllo turno (usato nei test)
    private final List<String> players;
    private int turnIndex = 0;

    /** Costruttore per test: nessun giocatore, nessun controllo turno. */
    public HangmanState(String word) {
        this(word, List.of());
    }

    /** Costruttore per produzione con lista giocatori ordinata. */
    public HangmanState(String word, List<String> players) {
        this.word = word.toLowerCase();
        this.players = List.copyOf(players);
    }

    /** Username del giocatore di turno, null se turni non attivi. */
    public synchronized String currentTurn() {
        if (players.isEmpty()) return null;
        return players.get(turnIndex % players.size());
    }

    /** True se è il turno di questo giocatore (sempre true se turni non attivi). */
    public synchronized boolean isMyTurn(String player) {
        return players.isEmpty() || player.equals(currentTurn());
    }

    /**
     * Prova una lettera da parte di un giocatore specifico.
     * Ritorna false se non è il suo turno, lettera non valida, duplicata, o partita finita.
     */
    public synchronized boolean guess(char input, String player) {
        if (!isMyTurn(player)) return false;
        char c = Character.toLowerCase(input);
        if (status != Status.PLAYING) return false;
        if (c < 'a' || c > 'z') return false;
        if (guessed.contains(c) || wrong.contains(c)) return false;

        if (word.indexOf(c) >= 0) {
            guessed.add(c);
            if (isComplete()) status = Status.WON;
        } else {
            wrong.add(c);
            if (wrong.size() >= MAX_WRONG) status = Status.LOST;
        }
        if (!players.isEmpty()) turnIndex++;
        return true;
    }

    /** Versione senza controllo turno — per test con costruttore no-players. */
    public synchronized boolean guess(char input) {
        char c = Character.toLowerCase(input);
        if (status != Status.PLAYING) return false;
        if (c < 'a' || c > 'z') return false;
        if (guessed.contains(c) || wrong.contains(c)) return false;

        if (word.indexOf(c) >= 0) {
            guessed.add(c);
            if (isComplete()) status = Status.WON;
        } else {
            wrong.add(c);
            if (wrong.size() >= MAX_WRONG) status = Status.LOST;
        }
        return true;
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
