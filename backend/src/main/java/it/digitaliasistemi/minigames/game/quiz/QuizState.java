package it.digitaliasistemi.minigames.game.quiz;

import java.util.*;

/**
 * Stato puro di una partita Quiz. Nessuna dipendenza da framework: testabile con JUnit puro.
 * La logica e' sincronizzata: piu' giocatori possono rispondere contemporaneamente.
 */
public class QuizState {

    public enum Phase { QUESTION, REVEAL }

    private final List<QuizQuestions.Question> questions; // 5 selezionate
    private int qIndex = 0;
    private Phase phase = Phase.QUESTION;

    /** Risposte registrate per la domanda corrente: email -> indice scelto. */
    private final Map<String, Integer> answers = new LinkedHashMap<>();

    /** Punteggi accumulati per tutta la partita: email -> punti. */
    private final Map<String, Integer> scores;

    /**
     * Costruisce lo stato con una lista di domande gia' scelta e azzera i punteggi
     * dei giocatori presenti.
     */
    public QuizState(List<QuizQuestions.Question> questions, Set<String> players) {
        if (questions.size() != 5) throw new IllegalArgumentException("Servono esattamente 5 domande");
        this.questions = List.copyOf(questions);
        this.scores = new LinkedHashMap<>();
        for (String email : players) scores.put(email, 0);
    }

    /** Seleziona 5 domande random senza ripetizioni dalla banca completa. */
    public static List<QuizQuestions.Question> randomSelection() {
        List<QuizQuestions.Question> all = new ArrayList<>(QuizQuestions.ALL);
        Collections.shuffle(all);
        return all.subList(0, 5);
    }

    // -------------------------------------------------------------------------
    // Azioni
    // -------------------------------------------------------------------------

    /**
     * Registra la risposta di un giocatore per la domanda corrente.
     * Ignora le risposte successive alla prima e quelle fuori fase.
     * Ritorna true se la risposta e' stata registrata, false se ignorata.
     */
    public synchronized boolean registerAnswer(String email, int optionIndex) {
        if (phase != Phase.QUESTION) return false;
        if (answers.containsKey(email)) return false; // gia' risposto
        answers.put(email, optionIndex);
        return true;
    }

    /**
     * Passa alla fase REVEAL: calcola i punteggi e li aggiorna.
     * Deve essere chiamato dal engine quando tutti hanno risposto.
     */
    public synchronized void revealAnswers() {
        if (phase != Phase.QUESTION) return;
        int correct = currentQuestion().correctIndex();
        for (Map.Entry<String, Integer> e : answers.entrySet()) {
            if (e.getValue() == correct) {
                scores.merge(e.getKey(), 1, Integer::sum);
            }
        }
        phase = Phase.REVEAL;
    }

    /**
     * Avanza alla domanda successiva (torna a QUESTION).
     * Ritorna true se c'e' ancora una domanda, false se la partita e' finita.
     */
    public synchronized boolean nextQuestion() {
        if (phase != Phase.REVEAL) return false;
        qIndex++;
        if (qIndex >= questions.size()) return false;
        answers.clear();
        phase = Phase.QUESTION;
        return true;
    }

    // -------------------------------------------------------------------------
    // Stato pubblico (lettura)
    // -------------------------------------------------------------------------

    public synchronized Phase phase() { return phase; }
    public synchronized int qIndex() { return qIndex; }
    public int total() { return questions.size(); }

    public synchronized QuizQuestions.Question currentQuestion() {
        return questions.get(qIndex);
    }

    /** Copia delle risposte correnti: email -> indice. */
    public synchronized Map<String, Integer> answers() { return new LinkedHashMap<>(answers); }

    /** Copia dei punteggi: email -> punti. */
    public synchronized Map<String, Integer> scores() { return new LinkedHashMap<>(scores); }

    /** Insiemi delle email che hanno gia' risposto alla domanda corrente. */
    public synchronized Set<String> answeredEmails() { return new LinkedHashSet<>(answers.keySet()); }

    /**
     * Classifica finale ordinata decrescente per punteggio.
     * Usata quando la partita e' terminata.
     */
    public synchronized List<Map<String, Object>> ranking() {
        return scores.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .map(e -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("username", e.getKey());
                entry.put("score", e.getValue());
                return entry;
            })
            .toList();
    }

    /**
     * Controlla se tutti i giocatori (insiemi dei punteggi, che corrisponde
     * all'insieme dei players registrati all'avvio) hanno risposto.
     */
    public synchronized boolean allAnswered(Set<String> currentPlayers) {
        return currentPlayers.stream().allMatch(answers::containsKey);
    }
}
