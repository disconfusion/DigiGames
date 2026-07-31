package it.digitaliasistemi.minigames.words;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;

/**
 * Dizionario online delle parole (<a href="https://random-word-api.herokuapp.com/home">random-word-api</a>).
 *
 * <p>Usa {@code /word?lang=<lingua>&length=<n>}: la lunghezza è la leva di difficoltà (vedi
 * {@link WordDifficulty}). Ogni tentativo ha {@value #TIMEOUT_SECONDS} s di tempo; chi chiama
 * decide quanti tentativi fare e cosa usare come ripiego (vedi {@code WordService}).
 *
 * <p>Le parole vengono normalizzate (minuscole, diacritici rimossi) e accettate solo se restano
 * di sole lettere a-z: l'impiccato non gestisce accenti, spazi o apostrofi.
 */
@ApplicationScoped
public class DictionaryClient {

    private static final Logger LOG = Logger.getLogger(DictionaryClient.class);

    /** Tempo massimo per singolo tentativo. */
    public static final int TIMEOUT_SECONDS = 10;

    /** Lingue del dizionario online (l'inglese è il default e non va passato). */
    public static final List<String> LANGUAGES = List.of("it", "en", "es", "fr", "de", "pt-br", "ro");
    public static final String DEFAULT_LANGUAGE = "it";

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Random rnd = new Random();

    // @Inject esplicito: la classe ha anche il costruttore per i test, e senza annotazione
    // Arc non saprebbe quale usare ("does not declare a valid bean constructor").
    @jakarta.inject.Inject
    public DictionaryClient(
            @ConfigProperty(name = "words.api.base-url",
                            defaultValue = "https://random-word-api.herokuapp.com") String baseUrl) {
        this(baseUrl, Duration.ofSeconds(TIMEOUT_SECONDS));
    }

    /** Costruttore per i test: URL e timeout espliciti. */
    DictionaryClient(String baseUrl, Duration timeout) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Lingua valida? (le sconosciute vengono ricondotte al default dai chiamanti) */
    public static boolean isKnownLanguage(String lang) {
        return lang != null && LANGUAGES.contains(lang.toLowerCase(Locale.ROOT));
    }

    /**
     * Una parola della lingua e difficoltà richieste, o vuoto se il dizionario non risponde in
     * tempo, restituisce un errore o manda solo parole inutilizzabili.
     */
    public Optional<String> fetch(String lang, WordDifficulty difficulty) {
        String language = isKnownLanguage(lang) ? lang.toLowerCase(Locale.ROOT) : DEFAULT_LANGUAGE;
        int length = difficulty.randomLength(rnd);
        // number=5: se qualche parola non è utilizzabile (accenti, trattini) ne restano altre.
        StringBuilder url = new StringBuilder(baseUrl)
                .append("/word?number=5&length=").append(length);
        if (!"en".equals(language)) url.append("&lang=").append(language);

        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                LOG.warnf("Dizionario: HTTP %d per %s", res.statusCode(), url);
                return Optional.empty();
            }
            JsonNode arr = mapper.readTree(res.body());
            if (!arr.isArray()) return Optional.empty();
            for (JsonNode n : arr) {
                String w = sanitize(n.asText(""));
                if (w != null) return Optional.of(w);
            }
            return Optional.empty();
        } catch (Exception e) {
            // Timeout, DNS, 503, JSON invalido: per il chiamante è tutto "non disponibile".
            LOG.debugf("Dizionario non disponibile (%s): %s", url, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Minuscole + rimozione diacritici; ritorna null se la parola non è di sole lettere a-z
     * o è troppo corta/lunga per l'impiccato.
     */
    static String sanitize(String raw) {
        if (raw == null) return null;
        String w = Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return w.matches("[a-z]{3,20}") ? w : null;
    }
}
