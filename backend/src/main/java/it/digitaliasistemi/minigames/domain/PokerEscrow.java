package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.List;

/**
 * Buyin in Token trattenuto per un tavolo di poker in corso.
 *
 * <p>Esiste una riga per ogni umano seduto a un tavolo con posta in Token, dal momento
 * dell'addebito fino alla conclusione della partita: il montepremi non è un numero in memoria
 * ma queste righe. Serve perché le stanze vivono in memoria e il processo può morire (restart,
 * cold-start di Render) con la partita a metà: al boot ogni riga rimasta è per definizione
 * orfana — la sua stanza non esiste più — e viene rimborsata (vedi {@code PokerEscrowService}).
 *
 * <p>Tabella separata, mai colonne nuove su {@code app_user}: in prod lo schema è in
 * {@code update}, che crea tabelle mancanti ma non aggiunge colonne in modo affidabile.
 */
@Entity
@Table(name = "poker_escrow",
       uniqueConstraints = @UniqueConstraint(columnNames = {"roomCode", "username"}))
public class PokerEscrow extends PanacheEntity {

    @Column(nullable = false)
    public String roomCode;

    @Column(nullable = false)
    public String username;

    /** Token trattenuti (il buyin del tavolo). */
    @Column(nullable = false)
    public int amount;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public static List<PokerEscrow> forRoom(String roomCode) {
        return list("roomCode", roomCode);
    }

    public static long deleteForRoom(String roomCode) {
        return delete("roomCode", roomCode);
    }
}
