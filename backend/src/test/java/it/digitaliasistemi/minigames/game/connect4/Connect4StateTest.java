package it.digitaliasistemi.minigames.game.connect4;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per Connect4State. JUnit puro — nessuna dipendenza da Quarkus.
 */
class Connect4StateTest {

    private static final String P1 = "alice@example.com"; // colore R
    private static final String P2 = "bob@example.com";   // colore Y

    // ---- Drop con gravità ----

    @Test
    void dropFallsToBottom() {
        var cs = new Connect4State(P1, P2);
        cs.drop(P1, 3);
        String[][] board = cs.board();
        assertEquals("R", board[5][3], "Il disco deve atterrare sulla riga più bassa");
        for (int r = 0; r < 5; r++) assertNull(board[r][3], "Le righe sopra devono essere vuote");
    }

    @Test
    void dropStacksUpward() {
        var cs = new Connect4State(P1, P2);
        cs.drop(P1, 0); // R -> riga 5
        cs.drop(P2, 0); // Y -> riga 4
        cs.drop(P1, 0); // R -> riga 3
        String[][] board = cs.board();
        assertEquals("R", board[5][0]);
        assertEquals("Y", board[4][0]);
        assertEquals("R", board[3][0]);
    }

    // ---- Turni alternati ----

    @Test
    void turnAlternatesBetweenPlayers() {
        var cs = new Connect4State(P1, P2);
        assertEquals(P1, cs.currentTurn());
        assertTrue(cs.drop(P1, 0));
        assertEquals(P2, cs.currentTurn());
        assertTrue(cs.drop(P2, 1));
        assertEquals(P1, cs.currentTurn());
    }

    // ---- Vittoria orizzontale ----

    @Test
    void horizontalWin() {
        var cs = new Connect4State(P1, P2);
        // P1 gioca colonne 0,1,2,3; P2 usa 4,5,6 per non interferire
        cs.drop(P1, 0); cs.drop(P2, 4);
        cs.drop(P1, 1); cs.drop(P2, 5);
        cs.drop(P1, 2); cs.drop(P2, 6);
        cs.drop(P1, 3); // quarto disco orizzontale
        assertEquals(Connect4State.Status.WON, cs.status());
        assertEquals(P1, cs.winner());
        assertNull(cs.currentTurn());
    }

    // ---- Vittoria verticale ----

    @Test
    void verticalWin() {
        var cs = new Connect4State(P1, P2);
        // P1 impila 4 in col 0; P2 scarica in col 1
        cs.drop(P1, 0); cs.drop(P2, 1);
        cs.drop(P1, 0); cs.drop(P2, 1);
        cs.drop(P1, 0); cs.drop(P2, 1);
        cs.drop(P1, 0);
        assertEquals(Connect4State.Status.WON, cs.status());
        assertEquals(P1, cs.winner());
    }

    // ---- Vittoria diagonale \ (alto-sinistra → basso-destra) ----
    //
    // Target P1: (5,0) (4,1) (3,2) (2,3)
    //
    // Sequenza mosse verificata:
    //   t1  P1→col0 → P1@(5,0)          diag[0]
    //   t2  P2→col1 → P2@(5,1)          base per col1
    //   t3  P1→col1 → P1@(4,1)          diag[1]
    //   t4  P2→col6 → P2@(5,6)          neutro
    //   t5  P1→col2 → P1@(5,2)          scarto (base per col2)
    //   t6  P2→col2 → P2@(4,2)          scarto
    //   t7  P1→col2 → P1@(3,2)          diag[2]
    //   t8  P2→col3 → P2@(5,3)          base per col3
    //   t9  P1→col3 → P1@(4,3)          scarto
    //   t10 P2→col3 → P2@(3,3)          scarto
    //   t11 P1→col3 → P1@(2,3)          diag[3] → WIN
    @Test
    void diagonalWinDownRight() {
        var cs = new Connect4State(P1, P2);
        cs.drop(P1, 0); cs.drop(P2, 1);
        cs.drop(P1, 1); cs.drop(P2, 6);
        cs.drop(P1, 2); cs.drop(P2, 2);
        cs.drop(P1, 2); cs.drop(P2, 3);
        cs.drop(P1, 3); cs.drop(P2, 3);
        cs.drop(P1, 3); // P1@(2,3) → vittoria diagonale \
        assertEquals(Connect4State.Status.WON, cs.status());
        assertEquals(P1, cs.winner());
    }

    // ---- Vittoria diagonale / (basso-sinistra → alto-destra) ----
    //
    // Target P1: (5,3) (4,2) (3,1) (2,0)
    //
    // Sequenza mosse verificata:
    //   t1  P1→col3 → P1@(5,3)          diag[0]
    //   t2  P2→col2 → P2@(5,2)          base per col2
    //   t3  P1→col2 → P1@(4,2)          diag[1]
    //   t4  P2→col6 → P2@(5,6)          neutro
    //   t5  P1→col1 → P1@(5,1)          scarto (base per col1)
    //   t6  P2→col1 → P2@(4,1)          scarto
    //   t7  P1→col1 → P1@(3,1)          diag[2]
    //   t8  P2→col0 → P2@(5,0)          base per col0
    //   t9  P1→col0 → P1@(4,0)          scarto
    //   t10 P2→col0 → P2@(3,0)          scarto
    //   t11 P1→col0 → P1@(2,0)          diag[3] → WIN
    @Test
    void diagonalWinUpRight() {
        var cs = new Connect4State(P1, P2);
        cs.drop(P1, 3); cs.drop(P2, 2);
        cs.drop(P1, 2); cs.drop(P2, 6);
        cs.drop(P1, 1); cs.drop(P2, 1);
        cs.drop(P1, 1); cs.drop(P2, 0);
        cs.drop(P1, 0); cs.drop(P2, 0);
        cs.drop(P1, 0); // P1@(2,0) → vittoria diagonale /
        assertEquals(Connect4State.Status.WON, cs.status());
        assertEquals(P1, cs.winner());
    }

    // ---- Pareggio (board piena senza vincitore) ----
    //
    // Board target verificata (nessun run di 4 in orizzontale, verticale o diagonale):
    //
    //   col:  0  1  2  3  4  5  6
    //   r0:   R  R  Y  Y  R  R  Y    (top)
    //   r1:   Y  Y  R  R  Y  Y  R
    //   r2:   R  R  Y  Y  R  R  Y
    //   r3:   Y  Y  R  R  Y  Y  R
    //   r4:   R  R  Y  Y  R  R  Y
    //   r5:   Y  Y  R  R  Y  Y  R    (bottom)
    //
    // Orizzontale: pattern RRYYRRY → max run = 2 ✓
    // Verticale:   ogni colonna alterna coppie RR/YY → max run = 2 ✓
    // Diagonale \: (r5,0)Y,(r4,1)R,(r3,2)R,(r2,3)Y → Y,R,R,Y → max=2 ✓
    //              (r5,1)Y,(r4,2)Y,(r3,3)R,(r2,4)R → Y,Y,R,R → max=2 ✓
    //              (r5,2)R,(r4,3)Y,(r3,4)Y,(r2,5)R → R,Y,Y,R → max=2 ✓
    // Diagonale /: simmetrica → max=2 ✓
    @Test
    void drawWhenBoardFull() {
        String[][] board = {
            {"R","R","Y","Y","R","R","Y"},
            {"Y","Y","R","R","Y","Y","R"},
            {"R","R","Y","Y","R","R","Y"},
            {"Y","Y","R","R","Y","Y","R"},
            {"R","R","Y","Y","R","R","Y"},
            {"Y","Y","R","R","Y","Y","R"},
        };
        var cs = new Connect4State(P1, P2, board);
        assertEquals(Connect4State.Status.DRAW, cs.status());
        assertNull(cs.winner());
        assertNull(cs.currentTurn());
    }

    // ---- Mossa illegale: fuori turno ----

    @Test
    void moveRejectedWhenNotYourTurn() {
        var cs = new Connect4State(P1, P2);
        assertFalse(cs.drop(P2, 0), "P2 non può muovere al primo turno");
        assertEquals(P1, cs.currentTurn(), "Il turno non deve cambiare");
    }

    @Test
    void moveRejectedWhenPlayingOutOfTurn() {
        var cs = new Connect4State(P1, P2);
        cs.drop(P1, 0); // turno passa a P2
        assertFalse(cs.drop(P1, 1), "P1 non può muovere due volte di fila");
        assertEquals(P2, cs.currentTurn());
    }

    // ---- Mossa illegale: colonna piena ----

    @Test
    void moveRejectedAfterGameOver() {
        var cs = new Connect4State(P1, P2);
        // P1 vince verticalmente in col 0
        cs.drop(P1, 0); cs.drop(P2, 1);
        cs.drop(P1, 0); cs.drop(P2, 1);
        cs.drop(P1, 0); cs.drop(P2, 1);
        cs.drop(P1, 0);
        assertEquals(Connect4State.Status.WON, cs.status());
        assertFalse(cs.drop(P2, 2), "Mossa rifiutata dopo fine partita");
        assertFalse(cs.drop(P1, 3), "Mossa rifiutata dopo fine partita");
    }

    // ---- Colonna fuori range ----

    @Test
    void moveRejectedWhenColumnOutOfRange() {
        var cs = new Connect4State(P1, P2);
        assertFalse(cs.drop(P1, -1));
        assertFalse(cs.drop(P1, 7));
        assertEquals(P1, cs.currentTurn());
    }

    // ---- Colonna piena durante partita in corso ----

    @Test
    void moveRejectedWhenColumnFull() {
        var cs = new Connect4State(P1, P2);
        // Riempiamo col 2 con 6 dischi alternati (3R + 3Y) senza creare vittoria.
        // Turni che toccano col2: P1-col2, P2-col2, P1-col2, P2-col2, P1-col2, P2-col2
        // Gli altri turni usano colonne neutre (col5, col6) per rispettare l'alternanza.
        //
        // Sequenza (12 mosse):
        //   t0  P1→col2   r5=R
        //   t1  P2→col2   r4=Y
        //   t2  P1→col2   r3=R
        //   t3  P2→col2   r2=Y
        //   t4  P1→col2   r1=R
        //   t5  P2→col2   r0=Y  → col2 piena [R,Y,R,Y,R,Y] nessuna vittoria verticale
        // Dopo t5 è il turno di P1.
        cs.drop(P1, 2);
        cs.drop(P2, 2);
        cs.drop(P1, 2);
        cs.drop(P2, 2);
        cs.drop(P1, 2);
        cs.drop(P2, 2); // col2 piena
        assertEquals(Connect4State.Status.PLAYING, cs.status());
        // P1 tenta di droppare in col 2 (piena)
        assertFalse(cs.drop(P1, 2), "Mossa in colonna piena deve essere rifiutata");
    }
}
