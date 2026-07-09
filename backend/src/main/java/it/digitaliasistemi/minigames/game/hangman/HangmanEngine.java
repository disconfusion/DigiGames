package it.digitaliasistemi.minigames.game.hangman;

import com.fasterxml.jackson.databind.JsonNode;
import it.digitaliasistemi.minigames.game.GameContext;
import it.digitaliasistemi.minigames.game.GameEngine;
import it.digitaliasistemi.minigames.leaderboard.LeaderboardService;
import it.digitaliasistemi.minigames.rooms.Room;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class HangmanEngine implements GameEngine {

    @Inject
    LeaderboardService leaderboard;

    @Override public String slug() { return "hangman"; }
    @Override public int maxPlayers() { return 8; }

    @Override
    public void onJoin(GameContext ctx) {
        if (ctx.room().game instanceof HangmanState hs) {
            ctx.replyToSender(state(hs, null, null));
        }
    }

    @Override
    public void onMessage(GameContext ctx, String type, JsonNode payload) {
        Room room = ctx.room();
        switch (type) {
            case "game:start" -> {
                HangmanConfig cfg = HangmanConfig.fromJson(room.options);
                // Evita di riproporre la stessa parola due volte di fila nella stessa stanza.
                String prev = room.game instanceof HangmanState old ? old.word() : null;
                HangmanState hs = new HangmanState(HangmanWords.randomExcluding(prev), new ArrayList<>(room.players), cfg);
                room.game = hs;
                room.status = Room.Status.PLAYING;
                ctx.broadcast(state(hs, ctx.senderEmail(), null));
            }
            case "guess" -> {
                if (!(room.game instanceof HangmanState hs)) {
                    ctx.replyToSender(error("Partita non avviata"));
                    return;
                }
                if (!hs.isMyTurn(ctx.senderEmail())) {
                    ctx.replyToSender(error("Non è il tuo turno"));
                    return;
                }
                String letter = payload.path("letter").asText("").toLowerCase();
                if (letter.length() != 1 || letter.charAt(0) < 'a' || letter.charAt(0) > 'z') {
                    ctx.replyToSender(error("Lettera non valida"));
                    return;
                }
                if (hs.guess(letter.charAt(0), ctx.senderEmail())) {
                    ctx.broadcast(state(hs, ctx.senderEmail(), letter));
                    finishIfOver(ctx, room, hs);
                }
            }
            case "guessWord" -> {
                if (!(room.game instanceof HangmanState hs)) {
                    ctx.replyToSender(error("Partita non avviata"));
                    return;
                }
                if (!hs.isMyTurn(ctx.senderEmail())) {
                    ctx.replyToSender(error("Non è il tuo turno"));
                    return;
                }
                String attempt = payload.path("word").asText("");
                if (hs.guessWord(attempt, ctx.senderEmail())) {
                    ctx.broadcast(state(hs, ctx.senderEmail(), null));
                    finishIfOver(ctx, room, hs);
                } else {
                    ctx.replyToSender(error("Tentativo non valido"));
                }
            }
            default -> ctx.replyToSender(error("Azione sconosciuta: " + type));
        }
    }

    /** Se la partita è finita: broadcast game:over, chiude la stanza e registra i risultati. */
    private void finishIfOver(GameContext ctx, Room room, HangmanState hs) {
        if (hs.status() == HangmanState.Status.PLAYING) return;
        ctx.broadcast(over(hs));
        room.status = Room.Status.DONE;
        boolean won = hs.status() == HangmanState.Status.WON;
        for (String p : room.players) {
            // Gli eliminati perdono anche se la parola viene indovinata da un altro giocatore.
            String r = won && !hs.isEliminated(p) ? "WIN" : "LOSE";
            leaderboard.record(p, "hangman", r);
        }
    }

    private Map<String, Object> state(HangmanState hs, String by, String letter) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:state");
        m.put("game", "hangman");
        m.put("masked", hs.masked());
        m.put("wrong", hs.wrong().stream().map(String::valueOf).toList());
        m.put("guessed", hs.guessed().stream().map(String::valueOf).toList());
        m.put("wrongCount", hs.wrongCount());
        m.put("maxWrong", hs.maxWrong());
        m.put("accessories", hs.accessories());
        m.put("maxVowels", hs.maxVowels());
        m.put("vowelsCalled", hs.vowelsCalled());
        m.put("lettersPerPlayer", hs.lettersPerPlayer());
        m.put("lettersUsed", hs.lettersUsed());
        m.put("eliminated", hs.eliminated().stream().toList());
        m.put("winner", hs.winner());
        m.put("status", hs.status().name());
        m.put("currentTurn", hs.currentTurn());
        if (by != null) m.put("lastBy", by);
        if (letter != null) m.put("lastLetter", letter);
        return m;
    }

    private Map<String, Object> over(HangmanState hs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "game:over");
        m.put("status", hs.status().name());
        m.put("word", hs.word());
        m.put("winner", hs.winner());
        m.put("eliminated", hs.eliminated().stream().toList());
        return m;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "error");
        m.put("message", msg);
        return m;
    }
}
