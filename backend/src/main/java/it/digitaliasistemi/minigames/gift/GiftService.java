package it.digitaliasistemi.minigames.gift;

import it.digitaliasistemi.minigames.domain.Gift;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Regali pendenti (assegnati dall'admin) mostrati all'utente al prossimo accesso. */
@ApplicationScoped
public class GiftService {

    /** Registra un regalo pendente per l'utente. */
    @Transactional
    public void record(String username, String type, String itemId, String label, int amount) {
        Gift g = new Gift();
        g.username = username;
        g.type = type;
        g.itemId = itemId;
        g.label = label;
        g.amount = amount;
        g.persist();
    }

    /** Regali pendenti dell'utente (come mappe per il JSON). */
    public List<Map<String, Object>> pending(String username) {
        return Gift.pendingFor(username).stream().map(g -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", g.type);
            m.put("itemId", g.itemId);
            m.put("label", g.label);
            m.put("amount", g.amount);
            return m;
        }).toList();
    }

    /** Segna come visti: cancella i regali pendenti dell'utente (storage-friendly). */
    @Transactional
    public void ack(String username) {
        Gift.delete("username", username);
    }
}
