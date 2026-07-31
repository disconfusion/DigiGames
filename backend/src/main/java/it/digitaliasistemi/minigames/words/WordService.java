package it.digitaliasistemi.minigames.words;

import it.digitaliasistemi.minigames.game.hangman.HangmanWords;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Sceglie le parole dell'impiccato: prima dal dizionario online, con le parole locali come
 * <b>ripiego</b>.
 *
 * <p>Politica dei tentativi (decisa col committente): due chiamate da
 * {@value DictionaryClient#TIMEOUT_SECONDS} s ciascuna; se nessuna riesce si prende una parola
 * locale della difficoltà richiesta. Nel caso peggiore sono ~20 s, per questo esiste
 * {@link #pickAsync}: chi la usa mostra un caricamento invece di bloccare la partita.
 */
@ApplicationScoped
public class WordService {

    private static final Logger LOG = Logger.getLogger(WordService.class);

    /** Tentativi sul dizionario online prima di ripiegare sulle parole locali. */
    public static final int ATTEMPTS = 2;
    /** Parole tenute pronte per ogni combinazione lingua+difficoltà già usata. */
    public static final int BUFFER_TARGET = 3;
    private static final int BUFFER_MAX = 5;

    @Inject DictionaryClient dictionary;

    private final Random rnd = new Random();
    private ExecutorService pool;

    /**
     * Parole già scaricate e pronte all'uso, per chiave {@code lingua|difficoltà}.
     * Il primo round paga l'attesa del dizionario; mentre si gioca la scorta si ricarica in
     * background, così i round successivi partono subito.
     */
    private final Map<String, Deque<String>> ready = new ConcurrentHashMap<>();
    /** Chiavi con un rifornimento già in corso: evita di accodare dieci fetch identiche. */
    private final Set<String> refilling = ConcurrentHashMap.newKeySet();

    private static String key(String lang, WordDifficulty diff) {
        return (lang == null ? DictionaryClient.DEFAULT_LANGUAGE : lang.toLowerCase()) + "|" + diff.key();
    }

    /**
     * Parola scelta.
     *
     * @param word       la parola (minuscola, sole lettere a-z)
     * @param difficulty difficoltà effettiva (dedotta dalla parola)
     * @param lang       lingua richiesta
     * @param fromApi    true se arriva dal dizionario online, false se è un ripiego locale
     */
    public record Pick(String word, WordDifficulty difficulty, String lang, boolean fromApi) {}

    @PostConstruct
    void start() {
        pool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "word-fetch");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    void stop() {
        if (pool != null) pool.shutdownNow();
    }

    /**
     * Sceglie una parola: due tentativi sul dizionario, poi ripiego locale. Bloccante fino a
     * ~{@value #ATTEMPTS}×{@value DictionaryClient#TIMEOUT_SECONDS} s: da usare solo fuori dai
     * percorsi interattivi (o dentro {@link #pickAsync}).
     */
    public Pick pickBlocking(String lang, WordDifficulty difficulty, String exclude) {
        WordDifficulty diff = difficulty != null ? difficulty : WordDifficulty.random(rnd);
        for (int i = 1; i <= ATTEMPTS; i++) {
            Optional<String> w = dictionary.fetch(lang, diff);
            if (w.isPresent() && !w.get().equals(exclude)) {
                return new Pick(w.get(), WordDifficulty.ofLength(w.get().length()), lang, true);
            }
            LOG.debugf("Dizionario: tentativo %d/%d senza esito (lang=%s, diff=%s)", i, ATTEMPTS, lang, diff.key());
        }
        String local = HangmanWords.randomInRange(diff.minLength(), diff.maxLength(), exclude);
        LOG.infof("Dizionario non disponibile: uso la parola locale \"%s\" (diff=%s)", local, diff.key());
        return new Pick(local, WordDifficulty.ofLength(local.length()), lang, false);
    }

    /** Solo parole locali (sorgente scelta dall'host: "locale"). */
    public Pick pickLocal(WordDifficulty difficulty, String exclude) {
        WordDifficulty diff = difficulty != null ? difficulty : WordDifficulty.random(rnd);
        String w = HangmanWords.randomInRange(diff.minLength(), diff.maxLength(), exclude);
        return new Pick(w, WordDifficulty.ofLength(w.length()), "it", false);
    }

    /**
     * Come {@link #pickBlocking} ma su un thread dedicato: il chiamante intanto mostra il
     * caricamento e riceve la parola nel callback. Le eccezioni del callback vengono loggate,
     * non propagate (girerebbero su un thread di servizio).
     */
    public void pickAsync(String lang, WordDifficulty difficulty, String exclude, Consumer<Pick> onDone) {
        pool.submit(() -> {
            try {
                onDone.accept(pickBlocking(lang, difficulty, exclude));
            } catch (Exception e) {
                LOG.error("Errore nella scelta asincrona della parola", e);
            }
        });
    }

    /**
     * Parola già pronta in scorta, se c'è: nessuna attesa di rete. Ritirandola si innesca il
     * rifornimento in background, così la scorta resta piena mentre i giocatori giocano.
     */
    public Optional<Pick> takeReady(String lang, WordDifficulty difficulty, String exclude) {
        if (difficulty == null) return Optional.empty();
        Deque<String> q = ready.get(key(lang, difficulty));
        if (q == null) return Optional.empty();
        String w;
        synchronized (q) {
            w = q.poll();
            if (w != null && w.equals(exclude)) {
                // Stessa parola del round precedente: rimettila in coda e prova la successiva.
                String next = q.poll();
                q.addLast(w);
                w = next;
            }
        }
        refill(lang, difficulty);
        if (w == null) return Optional.empty();
        return Optional.of(new Pick(w, WordDifficulty.ofLength(w.length()), lang, true));
    }

    /**
     * Riempie la scorta di questa combinazione fino a {@value #BUFFER_TARGET} parole, in background
     * e con un solo tentativo per parola: se il dizionario è giù non si insiste (ci riprova al
     * round successivo, e nel frattempo vale il ripiego locale).
     */
    public void refill(String lang, WordDifficulty difficulty) {
        if (difficulty == null) return;
        String k = key(lang, difficulty);
        if (!refilling.add(k)) return; // rifornimento già in corso
        pool.submit(() -> {
            try {
                Deque<String> q = ready.computeIfAbsent(k, x -> new ArrayDeque<>());
                while (size(q) < BUFFER_TARGET) {
                    Optional<String> w = dictionary.fetch(lang, difficulty);
                    if (w.isEmpty()) break;
                    synchronized (q) {
                        if (q.size() >= BUFFER_MAX || q.contains(w.get())) break;
                        q.addLast(w.get());
                    }
                }
            } catch (Exception e) {
                LOG.debugf("Rifornimento parole fallito per %s: %s", k, e.getMessage());
            } finally {
                refilling.remove(k);
            }
        });
    }

    private static int size(Deque<String> q) {
        synchronized (q) {
            return q.size();
        }
    }

    /** Parole attualmente in scorta per quella combinazione (diagnostica e test). */
    public int readyCount(String lang, WordDifficulty difficulty) {
        Deque<String> q = ready.get(key(lang, difficulty));
        return q == null ? 0 : size(q);
    }

    /** Difficoltà casuale: usata dalla Parola del Giorno, che ne estrae una nuova ogni giorno. */
    public WordDifficulty randomDifficulty() {
        return WordDifficulty.random(rnd);
    }
}
