package it.digitaliasistemi.minigames.game.poker;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import it.digitaliasistemi.minigames.domain.PokerEscrow;
import it.digitaliasistemi.minigames.rooms.Room;
import it.digitaliasistemi.minigames.rooms.RoomManager;
import it.digitaliasistemi.minigames.token.TokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Custodia dei buyin in Token dei tavoli di poker: trattiene all'avvio, paga il montepremi a
 * fine partita, rimborsa tutto se la partita non arriva mai alla fine.
 *
 * <p>Un tavolo può morire senza concludersi in due modi, coperti entrambi:
 * <ul>
 *   <li><b>processo che riparte</b> (deploy, restart, cold-start di Render): le stanze sono in
 *       memoria, quindi al boot ogni riga di escrow è orfana → rimborso totale;</li>
 *   <li><b>stanza svuotata</b> a processo vivo (tutti chiudono la scheda, {@code RoomReaper} la
 *       rimuove): la scansione periodica trova righe la cui stanza non è più in gioco → rimborso.</li>
 * </ul>
 *
 * <p>Conseguenza voluta: nessuno perde Token per una partita che il sistema non ha portato a
 * termine. Chi invece abbandona un tavolo ancora vivo resta nel montepremi — il suo buyin lo
 * incassa chi vince il tavolo.
 *
 * <p>Assume una sola istanza dell'applicazione, come tutto il resto delle stanze in memoria.
 */
@ApplicationScoped
public class PokerEscrowService {

    private static final Logger LOG = Logger.getLogger(PokerEscrowService.class);

    @Inject TokenService tokens;
    @Inject RoomManager rooms;

    /** Chi non ha Token a sufficienza per il buyin (lista vuota = si può partire). */
    public List<String> whoCannotAfford(List<String> usernames, int buyin) {
        List<String> out = new ArrayList<>();
        if (buyin <= 0) return out;
        for (String u : usernames) {
            if (tokens.balance(u) < buyin) out.add(u);
        }
        return out;
    }

    /**
     * Trattiene il buyin di ogni giocatore e registra il montepremi.
     * Ritorna false senza toccare nulla se un addebito non va a buon fine.
     */
    @Transactional
    public boolean collect(String roomCode, List<String> usernames, int buyin) {
        if (buyin <= 0) return true;
        for (String u : usernames) {
            if (!tokens.spend(u, buyin)) {
                // Rollback della transazione: gli addebiti già fatti in questo giro non restano.
                throw new IllegalStateException("Token insufficienti per " + u);
            }
            PokerEscrow e = new PokerEscrow();
            e.roomCode = roomCode;
            e.username = u;
            e.amount = buyin;
            e.persist();
        }
        LOG.infof("Poker %s: trattenuto buyin %d Token da %d giocatori", roomCode, buyin, usernames.size());
        return true;
    }

    /** Montepremi attualmente trattenuto per la stanza. */
    public int prize(String roomCode) {
        return PokerEscrow.forRoom(roomCode).stream().mapToInt(e -> e.amount).sum();
    }

    /**
     * Chiude il tavolo: tutto il montepremi a chi ha vinto, se è un umano.
     * Se il tavolo lo vince un'IA il montepremi non torna a nessuno: lo incassa il banco.
     */
    @Transactional
    public int settle(String roomCode, String winner) {
        List<PokerEscrow> held = PokerEscrow.forRoom(roomCode);
        if (held.isEmpty()) return 0;
        int prize = held.stream().mapToInt(e -> e.amount).sum();
        boolean toHuman = winner != null && held.stream().anyMatch(e -> e.username.equals(winner));
        PokerEscrow.deleteForRoom(roomCode);
        if (toHuman) {
            tokens.award(winner, prize);
            LOG.infof("Poker %s: montepremi %d Token a %s", roomCode, prize, winner);
            return prize;
        }
        // Nessun umano ha vinto il tavolo: l'ha vinto un'IA, oppure gli umani sono usciti tutti.
        LOG.infof("Poker %s: nessun vincitore umano, montepremi %d Token al banco", roomCode, prize);
        return 0;
    }

    /** Restituisce il buyin a tutti: la partita non è arrivata a una conclusione. */
    @Transactional
    public void refund(String roomCode) {
        refundRoom(roomCode);
    }

    /**
     * Al boot ogni escrow è orfano (le stanze vivono in memoria e non sopravvivono al restart):
     * si rimborsa tutto prima che qualcuno possa accorgersene.
     */
    @Transactional
    void refundOrphansAtBoot(@Observes StartupEvent ev) {
        for (String code : heldRooms()) refundRoom(code);
    }

    /** Escrow di tavoli che non sono più in gioco a processo vivo (stanza chiusa o svuotata). */
    @Scheduled(every = "60s")
    @Transactional
    void refundAbandoned() {
        for (String code : heldRooms()) {
            Room room = rooms.get(code);
            if (room == null || room.status != Room.Status.PLAYING) refundRoom(code);
        }
    }

    private Set<String> heldRooms() {
        Set<String> codes = new LinkedHashSet<>();
        for (PokerEscrow e : PokerEscrow.<PokerEscrow>listAll()) codes.add(e.roomCode);
        return codes;
    }

    /** Rimborso effettivo: privato perché l'auto-invocazione non attiverebbe {@code @Transactional}. */
    private void refundRoom(String roomCode) {
        List<PokerEscrow> held = PokerEscrow.forRoom(roomCode);
        if (held.isEmpty()) return;
        for (PokerEscrow e : held) tokens.award(e.username, e.amount);
        PokerEscrow.deleteForRoom(roomCode);
        LOG.infof("Poker %s: partita non conclusa, rimborsati %d buyin", roomCode, held.size());
    }
}
