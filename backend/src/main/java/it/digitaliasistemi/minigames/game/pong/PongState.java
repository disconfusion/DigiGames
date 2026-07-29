package it.digitaliasistemi.minigames.game.pong;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Stato puro del Pong: 2 giocatori, campo fisso, fisica avanzata a passi di tempo (nessun
 * framework, testabile con JUnit puro). Il server è l'autorità: il client invia solo la
 * <b>posizione desiderata</b> della propria racchetta, qui la racchetta ci si avvicina a
 * velocità limitata ({@value #PADDLE_SPEED} px/s) — niente teletrasporti.
 *
 * <p>Sistema di riferimento: campo {@value #W}×{@value #H} unità, origine in alto a sinistra.
 * Il frontend scala le unità sul canvas, così la fisica non dipende dalla risoluzione.
 */
public class PongState {

    public enum Status { PLAYING, WON }
    /** Lato del campo: L = sinistra, R = destra. */
    public enum Side { L, R }

    // ── Campo e corpi ────────────────────────────────────────────────────────
    public static final double W = 1000;
    public static final double H = 600;
    public static final double PADDLE_W = 14;
    public static final double PADDLE_H = 110;
    /** Distanza della faccia esterna della racchetta dal bordo. */
    public static final double PADDLE_MARGIN = 28;
    public static final double BALL_R = 9;

    // ── Velocità ─────────────────────────────────────────────────────────────
    /** Velocità della palla alla battuta (unità/s). */
    public static final double BALL_SPEED_START = 430;
    /** Accelerazione a ogni colpo di racchetta. */
    public static final double BALL_SPEED_GAIN = 1.055;
    public static final double BALL_SPEED_MAX = 1150;
    /** Velocità massima di inseguimento della racchetta verso la posizione richiesta. */
    public static final double PADDLE_SPEED = 780;
    /** Pausa (secondi) fra un punto e la battuta successiva. */
    public static final double SERVE_DELAY = 1.2;
    /** Angolo massimo di uscita dalla racchetta (rad, rispetto all'orizzontale). */
    private static final double MAX_BOUNCE_ANGLE = 1.0472; // 60°

    private final String playerL;
    private final String playerR;
    private final int pointsToWin;
    private final Random rnd;

    private double ballX = W / 2, ballY = H / 2;
    private double ballVx, ballVy;
    private double paddleLY = H / 2, paddleRY = H / 2;      // centro della racchetta
    private double targetLY = H / 2, targetRY = H / 2;      // posizione richiesta dal client
    private int scoreL, scoreR;
    private double serveIn = SERVE_DELAY;                   // secondi al lancio (0 = palla in gioco)
    private Side serveTo = Side.R;                          // verso chi parte la palla
    private Status status = Status.PLAYING;
    private String winner;
    /** Rimbalzi sulle racchette nello scambio corrente (statistica mostrata a fine punto). */
    private int rally;
    private int longestRally;

    public PongState(String playerL, String playerR, int pointsToWin) {
        this(playerL, playerR, pointsToWin, new Random());
    }

    /** Costruttore con Random iniettabile: rende i test deterministici. */
    public PongState(String playerL, String playerR, int pointsToWin, Random rnd) {
        this.playerL = playerL;
        this.playerR = playerR;
        this.pointsToWin = Math.max(1, Math.min(21, pointsToWin));
        this.rnd = rnd;
    }

    /** Posizione desiderata della racchetta (centro, in unità di campo). Ignora chi non gioca. */
    public synchronized void aim(String username, double y) {
        double clamped = clamp(y, PADDLE_H / 2, H - PADDLE_H / 2);
        if (playerL.equals(username)) targetLY = clamped;
        else if (playerR.equals(username)) targetRY = clamped;
    }

    /**
     * Avanza la simulazione di {@code dt} secondi.
     *
     * @return true se in questo passo è stato segnato un punto (il chiamante trasmette
     *         uno snapshot completo e controlla {@link #status()}).
     */
    public synchronized boolean tick(double dt) {
        if (status != Status.PLAYING) return false;

        paddleLY = follow(paddleLY, targetLY, dt);
        paddleRY = follow(paddleRY, targetRY, dt);

        // Pausa fra i punti: le racchette si muovono, la palla resta al centro.
        if (serveIn > 0) {
            serveIn -= dt;
            if (serveIn <= 0) {
                serveIn = 0;
                launch();
            }
            return false;
        }

        ballX += ballVx * dt;
        ballY += ballVy * dt;

        // Sponde alto/basso
        if (ballY < BALL_R) {
            ballY = BALL_R;
            ballVy = Math.abs(ballVy);
        } else if (ballY > H - BALL_R) {
            ballY = H - BALL_R;
            ballVy = -Math.abs(ballVy);
        }

        double faceL = PADDLE_MARGIN + PADDLE_W;   // superficie interna della racchetta sinistra
        double faceR = W - PADDLE_MARGIN - PADDLE_W;

        if (ballVx < 0 && ballX - BALL_R <= faceL && ballX > PADDLE_MARGIN - BALL_R) {
            if (hits(paddleLY)) {
                ballX = faceL + BALL_R;
                bounce(paddleLY, +1);
            }
        } else if (ballVx > 0 && ballX + BALL_R >= faceR && ballX < W - PADDLE_MARGIN + BALL_R) {
            if (hits(paddleRY)) {
                ballX = faceR - BALL_R;
                bounce(paddleRY, -1);
            }
        }

        // Punto: la palla ha superato il fondo campo
        if (ballX + BALL_R < 0) return point(Side.R);
        if (ballX - BALL_R > W) return point(Side.L);
        return false;
    }

    /** La palla è dentro l'altezza della racchetta il cui centro è {@code paddleY}? */
    private boolean hits(double paddleY) {
        return ballY >= paddleY - PADDLE_H / 2 - BALL_R && ballY <= paddleY + PADDLE_H / 2 + BALL_R;
    }

    /**
     * Rimbalzo sulla racchetta: l'angolo di uscita dipende da dove colpisci (centro = piatto,
     * estremità = angolo massimo), la velocità cresce di {@value #BALL_SPEED_GAIN}.
     *
     * @param dirX +1 rimbalzo verso destra, -1 verso sinistra
     */
    private void bounce(double paddleY, int dirX) {
        double offset = clamp((ballY - paddleY) / (PADDLE_H / 2), -1, 1);
        double speed = Math.min(Math.hypot(ballVx, ballVy) * BALL_SPEED_GAIN, BALL_SPEED_MAX);
        double angle = offset * MAX_BOUNCE_ANGLE;
        ballVx = dirX * speed * Math.cos(angle);
        ballVy = speed * Math.sin(angle);
        rally++;
    }

    /** Assegna il punto al lato indicato e prepara la battuta successiva (o chiude la partita). */
    private boolean point(Side to) {
        if (to == Side.L) scoreL++; else scoreR++;
        longestRally = Math.max(longestRally, rally);
        rally = 0;
        if (scoreL >= pointsToWin || scoreR >= pointsToWin) {
            status = Status.WON;
            winner = scoreL >= pointsToWin ? playerL : playerR;
            ballVx = ballVy = 0;
        } else {
            // Batte verso chi ha subito il punto: partenza sempre a suo favore.
            serveTo = to == Side.L ? Side.R : Side.L;
            resetBall();
        }
        return true;
    }

    private void resetBall() {
        ballX = W / 2;
        ballY = H / 2;
        ballVx = ballVy = 0;
        serveIn = SERVE_DELAY;
    }

    /** Lancia la palla verso {@link #serveTo} con un angolo iniziale contenuto. */
    private void launch() {
        double angle = (rnd.nextDouble() - 0.5) * 0.7; // ±0,35 rad
        int dir = serveTo == Side.R ? 1 : -1;
        ballVx = dir * BALL_SPEED_START * Math.cos(angle);
        ballVy = BALL_SPEED_START * Math.sin(angle);
    }

    /** Avvicina {@code cur} a {@code target} di al massimo PADDLE_SPEED·dt. */
    private static double follow(double cur, double target, double dt) {
        double max = PADDLE_SPEED * dt;
        double delta = clamp(target - cur, -max, max);
        return clamp(cur + delta, PADDLE_H / 2, H - PADDLE_H / 2);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /**
     * Chiude la partita perché un giocatore ha abbandonato: vince l'avversario.
     * Nessun effetto se la partita è già finita.
     */
    public synchronized void forfeit(String username) {
        if (status != Status.PLAYING) return;
        String other = playerL.equals(username) ? playerR : (playerR.equals(username) ? playerL : null);
        if (other == null) return;
        status = Status.WON;
        winner = other;
        ballVx = ballVy = 0;
    }

    // ── Getter per lo snapshot ───────────────────────────────────────────────
    public synchronized double ballX() { return ballX; }
    public synchronized double ballY() { return ballY; }
    public synchronized double paddleLY() { return paddleLY; }
    public synchronized double paddleRY() { return paddleRY; }
    public synchronized int scoreL() { return scoreL; }
    public synchronized int scoreR() { return scoreR; }
    /** Secondi residui prima della battuta (0 = palla in gioco). */
    public synchronized double serveIn() { return serveIn; }
    public synchronized Status status() { return status; }
    public synchronized String winner() { return winner; }
    public synchronized int rally() { return rally; }
    /** Velocità corrente della palla (unità/s): mostrata nell'HUD e utile nei test. */
    public synchronized double speed() { return Math.hypot(ballVx, ballVy); }
    public synchronized int longestRally() { return longestRally; }
    public int pointsToWin() { return pointsToWin; }
    public String playerL() { return playerL; }
    public String playerR() { return playerR; }

    /** Mappa username → lato ("L"/"R"), nell'ordine dei posti. */
    public Map<String, String> seats() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(playerL, Side.L.name());
        m.put(playerR, Side.R.name());
        return m;
    }
}
