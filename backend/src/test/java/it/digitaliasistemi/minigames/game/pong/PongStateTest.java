package it.digitaliasistemi.minigames.game.pong;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PongStateTest {

    private static final double DT = 1.0 / 60;

    /** Random deterministico: la battuta parte sempre con lo stesso angolo. */
    private static PongState fresh(int pointsToWin) {
        return new PongState("ann", "bob", pointsToWin, new Random(42));
    }

    /** Avanza n tick e riporta quanti punti sono stati segnati. */
    private static int run(PongState s, int ticks) {
        int points = 0;
        for (int i = 0; i < ticks; i++) if (s.tick(DT)) points++;
        return points;
    }

    @Test
    void startsWithServePauseAndStillBall() {
        var s = fresh(7);
        assertTrue(s.serveIn() > 0);
        run(s, 10);
        assertEquals(PongState.W / 2, s.ballX(), 0.001, "la palla resta al centro durante la pausa");
    }

    @Test
    void ballLaunchesAfterServeDelay() {
        var s = fresh(7);
        run(s, (int) Math.ceil(PongState.SERVE_DELAY / DT) + 5);
        assertTrue(Math.abs(s.ballX() - PongState.W / 2) > 1, "dopo la pausa la palla si muove");
    }

    @Test
    void paddleFollowsTargetAtLimitedSpeed() {
        var s = fresh(7);
        s.aim("ann", PongState.H); // richiesta oltre il bordo: viene limitata
        s.tick(DT);
        double expectedMax = PongState.H / 2 + PongState.PADDLE_SPEED * DT + 0.001;
        assertTrue(s.paddleLY() <= expectedMax, "la racchetta non si teletrasporta");
        assertTrue(s.paddleLY() > PongState.H / 2, "ma si muove verso il target");
        run(s, 600);
        assertEquals(PongState.H - PongState.PADDLE_H / 2, s.paddleLY(), 0.001,
                "a regime resta dentro il campo");
    }

    @Test
    void ignoresAimFromNonPlayer() {
        var s = fresh(7);
        double before = s.paddleLY();
        s.aim("carla", 0);
        s.tick(DT);
        assertEquals(before, s.paddleLY(), 0.001);
    }

    @Test
    void concedesPointWhenPaddleIsAway() {
        var s = fresh(3);
        // Entrambe le racchette in alto: la palla (che parte verso destra, quasi orizzontale) passa.
        s.aim("ann", 0);
        s.aim("bob", 0);
        int points = run(s, 60 * 5);
        assertTrue(points >= 1, "senza racchetta in mezzo si segna");
        assertTrue(s.scoreL() + s.scoreR() >= 1);
    }

    @Test
    void paddleBouncesBallBackAndSpeedsItUp() {
        var s = fresh(7);
        // Entrambe le racchette inseguono la palla: lo scambio non finisce mai.
        // 12 s bastano per più attraversamenti del campo (alla battuta ~430 u/s su 1000 u).
        for (int i = 0; i < 60 * 12; i++) {
            s.aim("ann", s.ballY());
            s.aim("bob", s.ballY());
            s.tick(DT);
        }
        assertEquals(0, s.scoreL() + s.scoreR(), "inseguendo la palla nessuno segna");
        assertTrue(s.rally() > 1, "lo scambio prosegue a colpi di racchetta");
        assertTrue(s.speed() > PongState.BALL_SPEED_START,
                "ogni colpo accelera la palla: " + s.speed());
        assertTrue(s.speed() <= PongState.BALL_SPEED_MAX, "la velocità resta sotto il tetto");
    }

    @Test
    void reachingPointsToWinEndsMatch() {
        var s = fresh(1); // basta un punto
        s.aim("ann", 0);
        s.aim("bob", 0);
        run(s, 60 * 6);
        assertEquals(PongState.Status.WON, s.status());
        assertTrue("ann".equals(s.winner()) || "bob".equals(s.winner()),
                "il vincitore è uno dei due giocatori");
    }

    @Test
    void pointsToWinIsClamped() {
        assertEquals(1, new PongState("ann", "bob", 0).pointsToWin());
        assertEquals(21, new PongState("ann", "bob", 999).pointsToWin());
        assertEquals(11, new PongState("ann", "bob", 11).pointsToWin());
    }

    @Test
    void forfeitGivesWinToOpponent() {
        var s = fresh(7);
        s.forfeit("ann");
        assertEquals(PongState.Status.WON, s.status());
        assertEquals("bob", s.winner());
        assertFalse(s.tick(DT), "a partita finita il tick non fa nulla");
    }

    @Test
    void forfeitOfNonPlayerIsIgnored() {
        var s = fresh(7);
        s.forfeit("carla");
        assertEquals(PongState.Status.PLAYING, s.status());
        assertNull(s.winner());
    }

    @Test
    void seatsMapPlayersToSides() {
        var s = fresh(7);
        assertEquals("L", s.seats().get("ann"));
        assertEquals("R", s.seats().get("bob"));
    }
}
