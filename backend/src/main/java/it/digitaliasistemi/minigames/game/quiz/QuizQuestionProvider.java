package it.digitaliasistemi.minigames.game.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fornisce le domande del quiz da sorgenti diverse, per dare varietà:
 *  - "local"  : banca IT interna ({@link QuizQuestions})
 *  - "opentdb": Open Trivia DB (opentdb.com, gratis, senza chiave, domande in inglese)
 *  - "mixed"  : metà locale + metà OpenTDB
 * In caso di errore di rete fa sempre fallback sulla banca locale.
 */
@ApplicationScoped
public class QuizQuestionProvider {

    private static final Logger LOG = Logger.getLogger(QuizQuestionProvider.class);
    private static final String OPENTDB_URL = "https://opentdb.com/api.php?amount=%d&type=multiple";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Inject
    ObjectMapper mapper;

    /** Seleziona {@code amount} domande dalla sorgente indicata, con fallback locale. */
    public List<QuizQuestions.Question> select(String source, int amount) {
        String src = source == null ? "local" : source.toLowerCase();
        return switch (src) {
            case "opentdb" -> {
                List<QuizQuestions.Question> remote = fetchRemote(amount);
                yield remote.size() == amount ? remote : QuizState.randomSelection();
            }
            case "mixed" -> {
                int remoteCount = amount / 2;
                List<QuizQuestions.Question> remote = fetchRemote(remoteCount);
                List<QuizQuestions.Question> result = new ArrayList<>(remote);
                List<QuizQuestions.Question> local = new ArrayList<>(QuizQuestions.ALL);
                Collections.shuffle(local);
                for (QuizQuestions.Question q : local) {
                    if (result.size() >= amount) break;
                    result.add(q);
                }
                Collections.shuffle(result);
                yield result.size() == amount ? result : QuizState.randomSelection();
            }
            default -> QuizState.randomSelection();
        };
    }

    private List<QuizQuestions.Question> fetchRemote(int amount) {
        if (amount <= 0) return List.of();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(OPENTDB_URL, amount)))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                LOG.warnf("OpenTDB ha risposto %d", res.statusCode());
                return List.of();
            }
            JsonNode root = mapper.readTree(res.body());
            if (root.path("response_code").asInt(-1) != 0) {
                LOG.warnf("OpenTDB response_code = %d", root.path("response_code").asInt(-1));
                return List.of();
            }
            List<QuizQuestions.Question> out = new ArrayList<>();
            for (JsonNode r : root.path("results")) {
                String text = decode(r.path("question").asText(""));
                String category = decode(r.path("category").asText("Trivia"));
                String correct = decode(r.path("correct_answer").asText(""));
                List<String> options = new ArrayList<>();
                options.add(correct);
                for (JsonNode inc : r.path("incorrect_answers")) {
                    options.add(decode(inc.asText("")));
                }
                if (options.size() != 4 || text.isBlank()) continue;
                Collections.shuffle(options);
                int correctIndex = options.indexOf(correct);
                out.add(new QuizQuestions.Question(text, category, options, correctIndex));
            }
            return out;
        } catch (Exception e) {
            LOG.warnf("Fetch OpenTDB fallito: %s — fallback su banca locale", e.getMessage());
            return List.of();
        }
    }

    /** Decodifica le entità HTML che OpenTDB usa nei testi (&quot; &#039; &amp; …). */
    static String decode(String s) {
        if (s == null) return "";
        return s.replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&apos;", "'")
                .replace("&rsquo;", "'")
                .replace("&lsquo;", "'")
                .replace("&ldquo;", "\"")
                .replace("&rdquo;", "\"")
                .replace("&ouml;", "ö")
                .replace("&eacute;", "é")
                .replace("&hellip;", "…")
                .replace("&shy;", "")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }
}
