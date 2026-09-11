package it.digitaliasistemi.minigames.game.poker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PokerHandsTest {

    // Ranghi: 0 = Due … 12 = Asso. Semi: 0..3.
    private static final int DUE = 0, TRE = 1, QUATTRO = 2, CINQUE = 3, SEI = 4, SETTE = 5,
            OTTO = 6, NOVE = 7, DIECI = 8, JACK = 9, DONNA = 10, RE = 11, ASSO = 12;

    private static int c(int rank, int suit) {
        return rank * PokerHands.SUITS + suit;
    }

    private static int score(int... cards) {
        return PokerHands.best(cards);
    }

    @Test
    void riconosceLeCategorie() {
        assertEquals(PokerHands.HIGH_CARD, PokerHands.category(
                score(c(ASSO, 0), c(RE, 1), c(NOVE, 2), c(SETTE, 3), c(DUE, 0))));
        assertEquals(PokerHands.PAIR, PokerHands.category(
                score(c(ASSO, 0), c(ASSO, 1), c(NOVE, 2), c(SETTE, 3), c(DUE, 0))));
        assertEquals(PokerHands.TWO_PAIR, PokerHands.category(
                score(c(ASSO, 0), c(ASSO, 1), c(NOVE, 2), c(NOVE, 3), c(DUE, 0))));
        assertEquals(PokerHands.TRIPS, PokerHands.category(
                score(c(ASSO, 0), c(ASSO, 1), c(ASSO, 2), c(NOVE, 3), c(DUE, 0))));
        assertEquals(PokerHands.STRAIGHT, PokerHands.category(
                score(c(CINQUE, 0), c(SEI, 1), c(SETTE, 2), c(OTTO, 3), c(NOVE, 0))));
        assertEquals(PokerHands.FLUSH, PokerHands.category(
                score(c(ASSO, 2), c(RE, 2), c(NOVE, 2), c(SETTE, 2), c(DUE, 2))));
        assertEquals(PokerHands.FULL_HOUSE, PokerHands.category(
                score(c(ASSO, 0), c(ASSO, 1), c(ASSO, 2), c(NOVE, 3), c(NOVE, 0))));
        assertEquals(PokerHands.QUADS, PokerHands.category(
                score(c(ASSO, 0), c(ASSO, 1), c(ASSO, 2), c(ASSO, 3), c(NOVE, 0))));
        assertEquals(PokerHands.STRAIGHT_FLUSH, PokerHands.category(
                score(c(DIECI, 1), c(JACK, 1), c(DONNA, 1), c(RE, 1), c(ASSO, 1))));
    }

    @Test
    void laScalaMinimaUsaLAssoComeUno() {
        int wheel = score(c(ASSO, 0), c(DUE, 1), c(TRE, 2), c(QUATTRO, 3), c(CINQUE, 0));
        int seiAlto = score(c(DUE, 0), c(TRE, 1), c(QUATTRO, 2), c(CINQUE, 3), c(SEI, 0));
        assertEquals(PokerHands.STRAIGHT, PokerHands.category(wheel));
        assertTrue(seiAlto > wheel, "la scala al Sei batte quella minima");
    }

    @Test
    void ordineDelleCategorie() {
        int coppia = score(c(DUE, 0), c(DUE, 1), c(SETTE, 2), c(NOVE, 3), c(JACK, 0));
        int doppiaCoppia = score(c(DUE, 0), c(DUE, 1), c(SETTE, 2), c(SETTE, 3), c(JACK, 0));
        int tris = score(c(DUE, 0), c(DUE, 1), c(DUE, 2), c(SETTE, 3), c(JACK, 0));
        int scala = score(c(CINQUE, 0), c(SEI, 1), c(SETTE, 2), c(OTTO, 3), c(NOVE, 0));
        int colore = score(c(DUE, 2), c(SETTE, 2), c(NOVE, 2), c(JACK, 2), c(RE, 2));
        int full = score(c(DUE, 0), c(DUE, 1), c(DUE, 2), c(SETTE, 3), c(SETTE, 0));
        int poker = score(c(DUE, 0), c(DUE, 1), c(DUE, 2), c(DUE, 3), c(SETTE, 0));
        int scalaColore = score(c(CINQUE, 1), c(SEI, 1), c(SETTE, 1), c(OTTO, 1), c(NOVE, 1));

        assertTrue(coppia < doppiaCoppia);
        assertTrue(doppiaCoppia < tris);
        assertTrue(tris < scala);
        assertTrue(scala < colore);
        assertTrue(colore < full);
        assertTrue(full < poker);
        assertTrue(poker < scalaColore);
    }

    @Test
    void aPariCategoriaDecideIlKicker() {
        int assiConRe = score(c(ASSO, 0), c(ASSO, 1), c(RE, 2), c(SETTE, 3), c(DUE, 0));
        int assiConDonna = score(c(ASSO, 0), c(ASSO, 1), c(DONNA, 2), c(SETTE, 3), c(DUE, 0));
        assertTrue(assiConRe > assiConDonna);

        int coppiaAlta = score(c(RE, 0), c(RE, 1), c(TRE, 2), c(QUATTRO, 3), c(CINQUE, 0));
        int coppiaBassa = score(c(DONNA, 0), c(DONNA, 1), c(ASSO, 2), c(RE, 3), c(JACK, 0));
        assertTrue(coppiaAlta > coppiaBassa, "conta prima la coppia, poi i kicker");
    }

    @Test
    void semiDiversiStessoPunteggio() {
        int uno = score(c(ASSO, 0), c(ASSO, 1), c(RE, 2), c(SETTE, 3), c(DUE, 0));
        int due = score(c(ASSO, 2), c(ASSO, 3), c(RE, 0), c(SETTE, 1), c(DUE, 1));
        assertEquals(uno, due, "i semi non hanno gerarchia");
    }

    @Test
    void suSetteCarteSceglieLeMiglioriCinque() {
        // Coperte: A♠ K♠ — board: Q♠ J♠ 10♠ 2♥ 2♦ → scala colore reale, non il full di due.
        int[] sette = {
            c(ASSO, 0), c(RE, 0), c(DONNA, 0), c(JACK, 0), c(DIECI, 0), c(DUE, 1), c(DUE, 2)
        };
        int best = PokerHands.best(sette);
        assertEquals(PokerHands.STRAIGHT_FLUSH, PokerHands.category(best));
        int[] five = PokerHands.bestFive(sette);
        assertEquals(5, five.length);
        for (int card : five) {
            assertEquals(0, PokerHands.suit(card), "le cinque carte scelte sono tutte di picche");
        }
    }

    @Test
    void etichetteLeggibili() {
        assertEquals("A", PokerHands.rankLabel(c(ASSO, 0)));
        assertEquals("10", PokerHands.rankLabel(c(DIECI, 1)));
        assertEquals("Colore", PokerHands.categoryName(
                score(c(DUE, 2), c(SETTE, 2), c(NOVE, 2), c(JACK, 2), c(RE, 2))));
    }
}
