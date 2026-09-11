package it.digitaliasistemi.minigames.game.poker;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PokerStateTest {

    private static PokerState table(int players, int chips, int smallBlind, long seed) {
        List<PokerState.Seat> seats = new ArrayList<>();
        for (int i = 0; i < players; i++) seats.add(new PokerState.Seat("p" + i, "P" + i, false));
        return new PokerState(seats, chips, smallBlind, 8, new Random(seed));
    }

    /** Fiches in tutti gli stack più quelle nel piatto: deve restare costante per tutta la partita. */
    private static int totalChips(PokerState st) {
        int total = st.pot();
        for (PokerState.Seat s : st.seats()) total += s.chips();
        return total;
    }

    @Test
    void iBuiVengonoMessiEIlTurnoParteDopoIlGrandeBuio() {
        PokerState st = table(3, 1000, 10, 1);
        st.startHand();

        assertEquals(30, st.pot(), "piccolo + grande buio");
        assertEquals(20, st.currentBet());
        assertEquals(10, st.seat("p1").bet(), "p1 è piccolo buio");
        assertEquals(20, st.seat("p2").bet(), "p2 è grande buio");
        assertEquals("p0", st.actor(), "parla il primo dopo il grande buio");
        assertEquals("p0", st.dealerId());
        for (PokerState.Seat s : st.seats()) assertEquals(2, s.hole().length);
    }

    @Test
    void headsUpIlBottoneEPiccoloBuioEParlaPerPrimo() {
        PokerState st = table(2, 500, 25, 2);
        st.startHand();

        assertEquals("p0", st.dealerId());
        assertEquals(25, st.seat("p0").bet(), "il bottone mette il piccolo buio");
        assertEquals(50, st.seat("p1").bet());
        assertEquals("p0", st.actor());
    }

    @Test
    void seTuttiPassanoIlPiattoVaAlGrandeBuio() {
        PokerState st = table(3, 1000, 10, 3);
        st.startHand();

        assertNull(st.act("p0", PokerState.Move.FOLD, 0));
        assertNull(st.act("p1", PokerState.Move.FOLD, 0));

        assertEquals(PokerState.Phase.HAND_OVER, st.phase());
        assertEquals(1010, st.seat("p2").chips(), "incassa i bui senza mostrare le carte");
        assertEquals(3000, totalChips(st));
        assertNotNull(st.lastHand());
        assertTrue(st.lastHand().revealed().isEmpty(), "nessuno showdown: niente carte mostrate");
    }

    @Test
    void mosseNonValideVengonoRifiutateSenzaCambiareStato() {
        PokerState st = table(3, 1000, 10, 4);
        st.startHand();
        int potPrima = st.pot();

        assertNotNull(st.act("p1", PokerState.Move.CHECK, 0), "non è il turno di p1");
        assertNotNull(st.act("p0", PokerState.Move.CHECK, 0), "c'è il grande buio da coprire");
        assertNotNull(st.act("p0", PokerState.Move.RAISE, 30), "rilancio sotto il minimo");
        assertNotNull(st.act("p0", PokerState.Move.RAISE, 5000), "più fiches di quante ne ha");
        assertEquals(potPrima, st.pot(), "nessuna mossa rifiutata tocca il piatto");
        assertEquals("p0", st.actor());

        assertNull(st.act("p0", PokerState.Move.RAISE, 40), "rilancio minimo valido");
        assertEquals(40, st.currentBet());
    }

    @Test
    void ilRilancioRiapreLAzione() {
        PokerState st = table(3, 1000, 10, 5);
        st.startHand();

        assertNull(st.act("p0", PokerState.Move.CALL, 0));
        assertNull(st.act("p1", PokerState.Move.CALL, 0));
        assertNull(st.act("p2", PokerState.Move.RAISE, 100), "il grande buio rilancia");
        assertEquals(PokerState.Street.PREFLOP, st.street(), "l'azione è riaperta, non si gira il flop");
        assertEquals("p0", st.actor());
    }

    @Test
    void ilFlopArrivaQuandoLePuntateSonoPari() {
        PokerState st = table(3, 1000, 10, 6);
        st.startHand();

        assertNull(st.act("p0", PokerState.Move.CALL, 0));
        assertNull(st.act("p1", PokerState.Move.CALL, 0));
        assertNull(st.act("p2", PokerState.Move.CHECK, 0));

        assertEquals(PokerState.Street.FLOP, st.street());
        assertEquals(3, st.board().length);
        assertEquals(60, st.pot());
        assertEquals(0, st.currentBet(), "nuova strada, puntate azzerate");
        assertEquals("p1", st.actor(), "postflop parla il primo dopo il bottone");
    }

    @Test
    void iSidePotNonPaganoAllAllInCortoPiuDelDovuto() {
        PokerState st = table(3, 1000, 10, 7);
        st.seat("p0").chips = 100;   // stack corto: potrà vincere solo il piatto principale
        st.startHand();

        assertNull(st.act("p0", PokerState.Move.RAISE, 100), "all-in corto");
        assertNull(st.act("p1", PokerState.Move.RAISE, 300));
        assertNull(st.act("p2", PokerState.Move.CALL, 0));
        // p1 e p2 si giocano il side pot: p0 non ha più voce in capitolo.
        while (st.phase() == PokerState.Phase.BETTING) {
            String actor = st.actor();
            assertNull(st.act(actor, PokerState.Move.CHECK, 0));
        }

        assertEquals(2100, totalChips(st), "100 + 1000 + 1000: nessuna fiche entra o esce");
        assertEquals(700, st.lastHand().pot(), "100 + 300 + 300 nel piatto");
        int p0won = st.lastHand().awards().stream()
                .filter(a -> a.seatId().equals("p0")).mapToInt(PokerState.Award::amount).sum();
        assertTrue(p0won <= 300, "il piatto principale vale 100 a testa: " + p0won);
        int assegnato = st.lastHand().awards().stream().mapToInt(PokerState.Award::amount).sum();
        assertEquals(700, assegnato, "tutto il piatto viene assegnato, nessuna fiche svanisce");
        assertEquals(0, st.pot(), "a mano chiusa il piatto è vuoto");
    }

    @Test
    void loStackAzzeratoEliminaEIlTorneoFinisceConUnVincitore() {
        PokerState st = table(2, 1000, 10, 8);
        st.seat("p1").chips = 40; // uno dei due è cortissimo: il duello finisce in poche mani
        st.startHand();

        int guard = 0;
        while (st.status() == PokerState.Status.PLAYING && guard++ < 50) {
            if (st.phase() == PokerState.Phase.HAND_OVER) {
                st.startHand();
                continue;
            }
            // Sempre all-in: il tavolo si decide senza dipendere dalle carte.
            String actor = st.actor();
            int max = st.maxRaiseTo(actor);
            PokerState.Move move = st.legalMoves(actor).contains(PokerState.Move.RAISE)
                    ? PokerState.Move.RAISE : PokerState.Move.CALL;
            assertNull(st.act(actor, move, max));
        }

        assertEquals(PokerState.Status.OVER, st.status());
        assertNotNull(st.winner());
        assertEquals(1040, st.seat(st.winner()).chips(), "il vincitore ha tutte le fiches");
        assertEquals(2, st.finishOrder().size());
        assertEquals(st.winner(), st.finishOrder().get(0));
        assertTrue(st.seat(st.finishOrder().get(1)).out(), "l'altro è eliminato");
    }

    @Test
    void leFichesSiConservanoPerTuttaLaPartita() {
        PokerState st = table(4, 500, 5, 99);
        Random rnd = new Random(1234);
        int start = 4 * 500;

        st.startHand();
        int guard = 0;
        while (st.status() == PokerState.Status.PLAYING && guard++ < 4000) {
            if (st.phase() == PokerState.Phase.HAND_OVER) {
                assertEquals(start, totalChips(st), "fine mano " + st.handNo());
                st.startHand();
                continue;
            }
            String actor = st.actor();
            assertNotNull(actor);
            List<PokerState.Move> legal = st.legalMoves(actor);
            assertTrue(legal.contains(PokerState.Move.FOLD), "si può sempre passare");
            PokerState.Move move = legal.get(rnd.nextInt(legal.size()));
            int amount = 0;
            if (move == PokerState.Move.RAISE) {
                int min = st.minRaiseTo(actor);
                int max = st.maxRaiseTo(actor);
                amount = min + (max > min ? rnd.nextInt(max - min + 1) : 0);
            }
            assertNull(st.act(actor, move, amount), "mossa legale rifiutata: " + move);
            assertEquals(start, totalChips(st), "dopo " + move);
        }

        assertEquals(PokerState.Status.OVER, st.status(), "la partita arriva a una fine");
        assertEquals(start, st.seat(st.winner()).chips(), "il vincitore raccoglie tutto");
    }

    @Test
    void chiLasciaIlTavoloEEliminatoELaManoProsegue() {
        PokerState st = table(3, 1000, 10, 11);
        st.startHand();

        st.forfeit("p0");
        assertTrue(st.seat("p0").out());
        assertTrue(st.seat("p0").folded());
        assertEquals(0, st.seat("p0").chips());
        assertTrue(st.finishOrder().contains("p0"));

        // Restano p1 e p2: la mano continua tra loro.
        if (st.status() == PokerState.Status.PLAYING && st.phase() == PokerState.Phase.BETTING) {
            assertTrue(List.of("p1", "p2").contains(st.actor()));
        }
    }
}
