package it.digitaliasistemi.minigames.game.quiz;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Unit test puro (NO @QuarkusTest) per QuizState. */
class QuizStateTest {

    // Domande fittizie con risposta corretta all'indice 1 per semplicita'
    private static final QuizQuestions.Question Q1 = new QuizQuestions.Question(
        "Domanda 1", "Test", List.of("A", "B", "C", "D"), 1
    );
    private static final QuizQuestions.Question Q2 = new QuizQuestions.Question(
        "Domanda 2", "Test", List.of("A", "B", "C", "D"), 1
    );
    private static final QuizQuestions.Question Q3 = new QuizQuestions.Question(
        "Domanda 3", "Test", List.of("A", "B", "C", "D"), 1
    );
    private static final QuizQuestions.Question Q4 = new QuizQuestions.Question(
        "Domanda 4", "Test", List.of("A", "B", "C", "D"), 1
    );
    private static final QuizQuestions.Question Q5 = new QuizQuestions.Question(
        "Domanda 5", "Test", List.of("A", "B", "C", "D"), 1
    );

    private static final List<QuizQuestions.Question> FIVE = List.of(Q1, Q2, Q3, Q4, Q5);

    private QuizState newState(String... emails) {
        return new QuizState(FIVE, Set.of(emails));
    }

    // -------------------------------------------------------------------------
    // Avvio: selezione 5 domande senza ripetizioni
    // -------------------------------------------------------------------------

    @Test
    void randomSelectionProducesFiveUniqueQuestions() {
        List<QuizQuestions.Question> sel = QuizState.randomSelection();
        assertEquals(5, sel.size());
        // nessuna ripetizione
        assertEquals(5, sel.stream().distinct().count());
        // tutte appartengono alla banca
        assertTrue(QuizQuestions.ALL.containsAll(sel));
    }

    @Test
    void constructorRequiresExactlyFiveQuestions() {
        assertThrows(IllegalArgumentException.class,
            () -> new QuizState(List.of(Q1, Q2, Q3), Set.of("a@test.it")));
    }

    @Test
    void initialStateIsQuestionPhaseAtIndex0() {
        QuizState qs = newState("alice@test.it");
        assertEquals(QuizState.Phase.QUESTION, qs.phase());
        assertEquals(0, qs.qIndex());
        assertEquals(5, qs.total());
    }

    @Test
    void scoresInitializedToZeroForAllPlayers() {
        QuizState qs = newState("alice@test.it", "bob@test.it");
        assertEquals(0, qs.scores().get("alice@test.it"));
        assertEquals(0, qs.scores().get("bob@test.it"));
    }

    // -------------------------------------------------------------------------
    // Registrazione risposta e punteggio
    // -------------------------------------------------------------------------

    @Test
    void correctAnswerIncrementsScore() {
        QuizState qs = newState("alice@test.it");
        // Q1 ha risposta corretta all'indice 1
        assertTrue(qs.registerAnswer("alice@test.it", 1));
        qs.revealAnswers();
        assertEquals(1, qs.scores().get("alice@test.it"));
    }

    @Test
    void wrongAnswerDoesNotIncrementScore() {
        QuizState qs = newState("alice@test.it");
        qs.registerAnswer("alice@test.it", 0); // sbagliato
        qs.revealAnswers();
        assertEquals(0, qs.scores().get("alice@test.it"));
    }

    // -------------------------------------------------------------------------
    // Doppia risposta ignorata
    // -------------------------------------------------------------------------

    @Test
    void secondAnswerFromSamePlayerIgnored() {
        QuizState qs = newState("alice@test.it");
        assertTrue(qs.registerAnswer("alice@test.it", 1));
        assertFalse(qs.registerAnswer("alice@test.it", 2)); // seconda risposta -> ignorata
        // solo la prima (corretta) deve comparire
        assertEquals(1, qs.answers().get("alice@test.it"));
    }

    // -------------------------------------------------------------------------
    // Passaggio QUESTION -> REVEAL quando tutti hanno risposto
    // -------------------------------------------------------------------------

    @Test
    void allAnsweredTransitionToReveal() {
        QuizState qs = newState("alice@test.it", "bob@test.it");
        assertFalse(qs.allAnswered(Set.of("alice@test.it", "bob@test.it")));
        qs.registerAnswer("alice@test.it", 1);
        assertFalse(qs.allAnswered(Set.of("alice@test.it", "bob@test.it")));
        qs.registerAnswer("bob@test.it", 0);
        assertTrue(qs.allAnswered(Set.of("alice@test.it", "bob@test.it")));

        qs.revealAnswers();
        assertEquals(QuizState.Phase.REVEAL, qs.phase());
        // alice corretta (+1), bob sbagliato (+0)
        assertEquals(1, qs.scores().get("alice@test.it"));
        assertEquals(0, qs.scores().get("bob@test.it"));
    }

    @Test
    void answeredEmailsTrackedCorrectly() {
        QuizState qs = newState("alice@test.it", "bob@test.it");
        qs.registerAnswer("alice@test.it", 1);
        assertTrue(qs.answeredEmails().contains("alice@test.it"));
        assertFalse(qs.answeredEmails().contains("bob@test.it"));
    }

    // -------------------------------------------------------------------------
    // Next: avanza alla domanda successiva
    // -------------------------------------------------------------------------

    @Test
    void nextAdvancesToNextQuestion() {
        QuizState qs = newState("alice@test.it");
        qs.registerAnswer("alice@test.it", 1);
        qs.revealAnswers();
        assertEquals(QuizState.Phase.REVEAL, qs.phase());

        boolean hasNext = qs.nextQuestion();
        assertTrue(hasNext);
        assertEquals(1, qs.qIndex());
        assertEquals(QuizState.Phase.QUESTION, qs.phase());
        // answers azzerate per la nuova domanda
        assertTrue(qs.answers().isEmpty());
    }

    @Test
    void nextReturnsFalseAfterLastQuestion() {
        QuizState qs = newState("alice@test.it");
        // gioca tutte le 5 domande
        for (int i = 0; i < 4; i++) {
            qs.registerAnswer("alice@test.it", 1);
            qs.revealAnswers();
            assertTrue(qs.nextQuestion(), "nextQuestion doveva avanzare alla domanda " + (i + 1));
        }
        // quinta domanda
        assertEquals(4, qs.qIndex());
        qs.registerAnswer("alice@test.it", 1);
        qs.revealAnswers();
        assertFalse(qs.nextQuestion()); // partita finita
    }

    // -------------------------------------------------------------------------
    // Classifica finale ordinata per punteggio decrescente
    // -------------------------------------------------------------------------

    @Test
    void rankingOrderedByScoreDescending() {
        QuizState qs = newState("alice@test.it", "bob@test.it", "carl@test.it");
        // alice risponde sempre bene, bob a meta', carl mai
        for (int i = 0; i < 5; i++) {
            qs.registerAnswer("alice@test.it", 1); // corretta
            qs.registerAnswer("bob@test.it", i < 2 ? 1 : 0); // 2 corrette, 3 sbagliate
            qs.registerAnswer("carl@test.it", 0); // sempre sbagliata
            qs.revealAnswers();
            if (i < 4) qs.nextQuestion();
        }
        var ranking = qs.ranking();
        assertEquals("alice@test.it", ranking.get(0).get("username"));
        assertEquals(5, ranking.get(0).get("score"));
        assertEquals("bob@test.it", ranking.get(1).get("username"));
        assertEquals(2, ranking.get(1).get("score"));
        assertEquals("carl@test.it", ranking.get(2).get("username"));
        assertEquals(0, ranking.get(2).get("score"));
    }

    // -------------------------------------------------------------------------
    // Risposte fuori fase ignorate
    // -------------------------------------------------------------------------

    @Test
    void answerInRevealPhaseIgnored() {
        QuizState qs = newState("alice@test.it");
        qs.registerAnswer("alice@test.it", 1);
        qs.revealAnswers();
        // tentiamo di rispondere durante REVEAL
        assertFalse(qs.registerAnswer("alice@test.it", 0));
    }
}
