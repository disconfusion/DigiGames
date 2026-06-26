package it.digitaliasistemi.minigames.token;

import it.digitaliasistemi.minigames.domain.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Gestione del saldo Token (valuta interna).
 * Solo saldo corrente su AppUser.tokens: nessuno storico (scelta di design alpha).
 */
@ApplicationScoped
public class TokenService {

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
        if (u != null) u.tokens += amount; // entità gestita: flush automatico a fine transazione
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
        return true;
    }
}
