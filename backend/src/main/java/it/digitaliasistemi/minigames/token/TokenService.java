package it.digitaliasistemi.minigames.token;

import it.digitaliasistemi.minigames.domain.AppUser;
import it.digitaliasistemi.minigames.ws.NotifyBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Map;

/**
 * Gestione del saldo Token (valuta interna).
 * Solo saldo corrente su AppUser.tokens: nessuno storico (scelta di design alpha).
 *
 * <p>Ogni variazione del saldo viene notificata al proprietario sul canale {@code /ws/notify}
 * ({@code {"type":"tokens","balance":N}}): il badge in header resta reattivo senza aspettare
 * il polling da 30s. Questo è il punto di passaggio unico per accrediti/addebiti — chi tocca
 * {@code AppUser.tokens} direttamente bypassa la notifica.
 */
@ApplicationScoped
public class TokenService {

    @Inject NotifyBus notifyBus;

    /** Saldo corrente dell'utente (0 se non trovato). */
    public int balance(String username) {
        AppUser u = AppUser.findByUsername(username);
        return u != null ? u.tokens : 0;
    }

    /** Accredita Token (importi <= 0 ignorati). */
    @Transactional
    public void award(String username, int amount) {
        if (amount <= 0) return;
        AppUser u = AppUser.findByUsername(username);
        if (u == null) return;
        u.tokens += amount; // entità gestita: flush automatico a fine transazione
        notifyBalance(username, u.tokens);
    }

    /**
     * Addebita Token per un acquisto. Ritorna true se il saldo era sufficiente
     * (e l'addebito è avvenuto), false altrimenti (saldo invariato).
     */
    @Transactional
    public boolean spend(String username, int amount) {
        if (amount <= 0) return true;
        AppUser u = AppUser.findByUsername(username);
        if (u == null || u.tokens < amount) return false;
        u.tokens -= amount;
        notifyBalance(username, u.tokens);
        return true;
    }

    /** Notifica il nuovo saldo al proprietario (best-effort: se non è connesso non fa nulla). */
    private void notifyBalance(String username, int balance) {
        notifyBus.push(username, "tokens", Map.of("balance", balance));
    }
}
