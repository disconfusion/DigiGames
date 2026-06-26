package it.digitaliasistemi.minigames.shop;

import it.digitaliasistemi.minigames.domain.OwnedPower;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** Inventario poteri per utente (quantità per powerId). */
@ApplicationScoped
public class InventoryService {

    /** Mappa powerId → quantità posseduta dall'utente. */
    public Map<String, Integer> owned(String username) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (OwnedPower op : OwnedPower.<OwnedPower>list("username", username)) {
            out.put(op.powerId, op.quantity);
        }
        return out;
    }

    public int quantity(String username, String powerId) {
        OwnedPower op = OwnedPower.find(username, powerId);
        return op != null ? op.quantity : 0;
    }

    /** Aggiunge n cariche del potere (crea la riga se non esiste). */
    @Transactional
    public void grant(String username, String powerId, int n) {
        if (n <= 0) return;
        OwnedPower op = OwnedPower.find(username, powerId);
        if (op == null) {
            op = new OwnedPower();
            op.username = username;
            op.powerId = powerId;
            op.quantity = n;
            op.persist();
        } else {
            op.quantity += n;
        }
    }

    /**
     * Consuma una carica del potere. Ritorna true se era disponibile (e consumata),
     * false se l'utente non ne possedeva (stato invariato).
     */
    @Transactional
    public boolean consume(String username, String powerId) {
        OwnedPower op = OwnedPower.find(username, powerId);
        if (op == null || op.quantity <= 0) return false;
        op.quantity -= 1;
        return true;
    }
}
