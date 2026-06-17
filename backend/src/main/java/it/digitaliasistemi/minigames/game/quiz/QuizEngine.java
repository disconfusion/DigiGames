package it.digitaliasistemi.minigames.game.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import it.digitaliasistemi.minigames.game.GameContext;
import it.digitaliasistemi.minigames.game.GameEngine;
import it.digitaliasistemi.minigames.leaderboard.LeaderboardService;
import it.digitaliasistemi.minigames.rooms.Room;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Quiz a risposta multipla, fino a 8 giocatori, 5 domande a partita. */
@ApplicationScoped
public class QuizEngine implements GameEngine {

    @Inject
    LeaderboardService leaderboard;

    @Inject
    QuizQuestionProvider questionProvider;

    @Override
    public String slug() {
        return "quiz";
    }

    @Override
    public int maxPlayers() {
        return 8;
    }

    @Override
    public void onJoin(GameContext ctx) {
        if (ctx.room().game instanceof QuizState qs) {
            ctx.replyToSender(buildSnapshot(qs));
        }
    }

    @Override
    public void onMessage(GameContext ctx, String type, JsonNode payload) {
        Room room = ctx.room();

        switch (type) {
            case "game:start" -> {
                String source = room.options != null ? room.options.path("source").asText("local") : "local";
                QuizState qs = new QuizState(questionProvider.select(source, 5), room.players);
                room.game = qs;
                room.status = Room.Status.PLAYING;
                ctx.broadcast(buildSnapshot(qs));
            }

            case "answer" -> {
                if (!(room.game instanceof QuizState qs)) {
                    ctx.replyToSender(error("Partita non avviata"));
                    return;
                }
                if (qs.phase() != QuizState.Phase.QUESTION) {
                    ctx.replyToSender(error("Non siamo in fase di risposta"));
                    return;
                }
                int index = payload.path("index").asInt(-1);
                if (index < 0 || index > 3) {
                    ctx.replyToSender(error("Indice risposta non valido"));
                    return;
                }
                boolean accepted = qs.registerAnswer(ctx.senderEmail(), index);
                if (!accepted) {
                    // doppia risposta o fuori fase: silenzio (ignora)
                    return;
                }
                // broadcast stato aggiornato con answeredEmails
                ctx.broadcast(buildSnapshot(qs));

                // se tutti i presenti hanno risposto -> REVEAL automatico
                if (qs.allAnswered(room.players)) {
                    qs.revealAnswers();
                    ctx.broadcast(buildSnapshot(qs));
                }
            }

            case "next" -> {
                if (!(room.game instanceof QuizState qs)) {
                    ctx.replyToSender(error("Partita non avviata"));
                    return;
                }
                if (qs.phase() != QuizState.Phase.REVEAL) {
                    ctx.replyToSender(error("Non siamo in fase di reveal"));
                    return;
                }
                boolean hasNext = qs.nextQuestion();
                if (hasNext) {
                    ctx.broadcast(buildSnapshot(qs));
                } else {
                    // partita finita
                    room.status = Room.Status.DONE;
                    ctx.broadcast(buildGameOver(qs));
                    List<Map<String, Object>> ranking = qs.ranking();
                    String winner = ranking.isEmpty() ? null : (String) ranking.get(0).get("username");
                    for (String p : room.players) {
                        leaderboard.record(p, "quiz", p.equals(winner) ? "WIN" : "LOSE");
                    }
                }
            }

            default -> ctx.replyToSender(error("Azione sconosciuta: " + type));
        }
    }

    // -------------------------------------------------------------------------
    // Costruttori snapshot
    // -------------------------------------------------------------------------

    private Map<String, Object> buildSnapshot(QuizState qs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:state");
        m.put("game", "quiz");
        // il frontend QuizBoard usa "phase"; manteniamo anche "status" per coerenza col protocollo
        m.put("phase", qs.phase().name());
        m.put("status", qs.phase().name());

        m.put("qIndex", qs.qIndex());
        m.put("total", qs.total());

        QuizQuestions.Question q = qs.currentQuestion();

        if (qs.phase() == QuizState.Phase.QUESTION) {
            // Non rivelare l'indice corretto in fase QUESTION
            Map<String, Object> questionMap = new LinkedHashMap<>();
            questionMap.put("text", q.text());
            questionMap.put("category", q.category());
            questionMap.put("options", q.options());
            m.put("question", questionMap);

            m.put("answeredEmails", List.copyOf(qs.answeredEmails()));
            m.put("playerCount", qs.scores().size());
            m.put("scores", qs.scores());

        } else {
            // Fase REVEAL: si rivela l'indice corretto e le risposte di tutti
            Map<String, Object> questionMap = new LinkedHashMap<>();
            questionMap.put("text", q.text());
            questionMap.put("category", q.category());
            questionMap.put("options", q.options());
            m.put("question", questionMap);

            m.put("correctIndex", q.correctIndex());
            m.put("answers", qs.answers());
            m.put("scores", qs.scores());
        }

        return m;
    }

    private Map<String, Object> buildGameOver(QuizState qs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:over");
        m.put("status", "DONE");
        m.put("ranking", qs.ranking());
        return m;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "error");
        m.put("message", msg);
        return m;
    }
}
