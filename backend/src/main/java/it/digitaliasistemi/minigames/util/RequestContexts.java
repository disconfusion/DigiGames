package it.digitaliasistemi.minigames.util;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;

/**
 * Esegue del codice con il contesto di richiesta CDI attivo.
 *
 * <p>Serve ai thread di servizio (loop di gioco del Pong, download delle parole): lì il contesto
 * di richiesta non esiste e ogni accesso Panache lancerebbe {@code ContextNotActiveException}.
 * Se il contesto è già attivo il blocco viene eseguito così com'è.
 */
public final class RequestContexts {

    private RequestContexts() {}

    public static void run(Runnable body) {
        ManagedContext ctx = Arc.container().requestContext();
        if (ctx.isActive()) {
            body.run();
            return;
        }
        ctx.activate();
        try {
            body.run();
        } finally {
            ctx.terminate();
        }
    }
}
