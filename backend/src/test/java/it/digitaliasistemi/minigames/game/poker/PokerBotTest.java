package it.digitaliasistemi.minigames.game.poker;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PokerBotTest {

    private static PokerState botTable(int players, int chips, long seed) {
        List<PokerState.Seat> seats = new ArrayList<>();
        for (int i = 0; i < players; i++) seats.add(new PokerState.Seat("bot:" + i, "Bot" + i, true));
        return new PokerState(seats, chips, Math.max(1, chips / 100), 8, new Random(seed));
    }

    /**
     * Un tavolo di soli bot deve arrivare a una conclusione senza che lo State rifiuti mai una
     * loro mossa: l'engine ha un ripiego, ma se scattasse vorrebbe dire che il bot propone mosse
     * fuori regola.
     */
    @Test
    void iBotGiocanoSoloMosseValideEIlTavoloSiChiude() {
        for (PokerBot.Level level : PokerBot.Level.values()) {
            PokerState st = botTable(4, 600, 7 + level.ordinal());
            Random rnd = new Random(31L + level.ordinal());
            st.startHand();

            int guard = 0;
            while (st.status() == PokerState.Status.PLAYING && guard++ < 8000) {
                if (st.phase() == PokerState.Phase.HAND_OVER) {
                    st.startHand();
                    continue;
                }
                String actor = st.actor();
                assertNotNull(actor, "con la mano in corso c'è sempre un posto di turno");
                PokerBot.Decision d = PokerBot.decide(st, actor, level, rnd);
                String refused = st.act(actor, d.move(), d.amount());
                assertEquals(null, refused,
                        "mossa illegale proposta dall'IA " + level + ": " + d.move() + " " + d.amount()
                                + " (" + refused + ")");
            }

            assertEquals(PokerState.Status.OVER, st.status(), "il tavolo IA " + level + " si chiude");
            assertEquals(4 * 600, st.seat(st.winner()).chips(), "il vincitore raccoglie tutte le fiches");
        }
    }

    @Test
    void ilPunteggioDiChenPremiaLeManiForti() {
        double assi = PokerBot.chen(new int[]{ card(12, 0), card(12, 1) });
        double reDonnaSuited = PokerBot.chen(new int[]{ card(11, 2), card(10, 2) });
        double settaDueSpaiate = PokerBot.chen(new int[]{ card(5, 0), card(0, 1) });

        assertTrue(assi > reDonnaSuited, "una coppia d'assi vale più di K-Q suited");
        assertTrue(reDonnaSuited > settaDueSpaiate, "K-Q suited vale più di 7-2 spaiate");
        assertTrue(settaDueSpaiate < 5, "la mano peggiore del poker resta bassa: " + settaDueSpaiate);
    }

    private static int card(int rank, int suit) {
        return rank * PokerHands.SUITS + suit;
    }
}
