package it.digitaliasistemi.minigames.game.hangman;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stato di una partita di impiccato. Logica pura (nessuna dipendenza da framework),
 * thread-safe sul metodo guess: piu' giocatori possono proporre lettere insieme.
 */
public class HangmanState {

    public enum Status { PLAYING, WON, LOST }

    public static final int MAX_WRONG = 6;

    private final String word; // lowercase
    private final Set<Character> guessed = new LinkedHashSet<>();
    private final Set<Character> wrong = new LinkedHashSet<>();
    private Status status = Status.PLAYING;

    public HangmanState(String word) {
        this.word = word.toLowerCase();
    }

    /** Prova una lettera. Ritorna true se accettata (nuova e valida), false se ignorata. */
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

    /** Parola mascherata: '_' per le lettere non ancora indovinate (rivelata se LOST). */
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
