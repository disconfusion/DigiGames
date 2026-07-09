package it.digitaliasistemi.minigames.rooms;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.websockets.next.OpenConnections;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rimuove le stanze fantasma: quelle rimaste senza alcun socket connesso oltre un periodo di grazia.
 * Copre i casi in cui la stanza resterebbe altrimenti residente in memoria per tutta la vita del
 * processo, invisibile nell'UI ma non più chiudibile: host che chiude la scheda a metà partita,
 * partite finite (DONE) mai chiuse con "Chiudi stanza", lobby abbandonate.
 *
 * <p>La grazia tollera i reconnect transienti del client (ws.ts riconnette con backoff): finché
 * esiste almeno un socket per il codice, la stanza è "viva" e il timer si azzera.
 */
@ApplicationScoped
public class RoomReaper {

    private static final Logger LOG = Logger.getLogger(RoomReaper.class);

    /** Grazia prima della rimozione: deve superare la finestra di reconnect del client. */
    private static final long GRACE_MS = 90_000;

    @Inject RoomManager rooms;
    @Inject OpenConnections connections;

    @Scheduled(every = "30s")
    void reap() {
        long now = System.currentTimeMillis();
        Set<String> liveCodes = connections.stream()
                .map(c -> c.pathParam("code"))
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        for (Room r : rooms.all()) {
            if (liveCodes.contains(r.code.toUpperCase())) {
                r.emptySince = 0;
            } else if (r.emptySince == 0) {
                r.emptySince = now; // prima volta senza connessioni: avvia il timer di grazia
            } else if (now - r.emptySince > GRACE_MS) {
                rooms.remove(r.code);
                LOG.infof("Stanza fantasma rimossa: %s (status=%s)", r.code, r.status);
            }
        }
    }
}
