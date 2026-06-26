package it.digitaliasistemi.minigames.shop;

import it.digitaliasistemi.minigames.domain.PowerPrice;
import it.digitaliasistemi.minigames.token.TokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Logica dello shop: prezzi (con override admin), catalogo per utente, acquisto. */
@ApplicationScoped
public class ShopService {

    @Inject TokenService tokens;
    @Inject InventoryService inventory;

    /** Prezzo corrente: override admin (PowerPrice) se presente, altrimenti default del catalogo. */
    public int priceOf(PowerCatalog.PowerDef def) {
        PowerPrice p = PowerPrice.findByPowerId(def.id());
        return p != null ? p.cost : def.defaultCost();
    }

    /** Catalogo completo con prezzo corrente e quantità posseduta dall'utente. */
    public List<Map<String, Object>> catalog(String username) {
        Map<String, Integer> owned = inventory.owned(username);
        List<Map<String, Object>> out = new ArrayList<>();
        for (PowerCatalog.PowerDef d : PowerCatalog.POWERS) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.id());
            m.put("game", d.game());
            m.put("label", d.label());
            m.put("emoji", d.emoji());
            m.put("description", d.description());
            m.put("usage", d.usage());
            m.put("phase", d.phase());
            m.put("cost", priceOf(d));
            m.put("owned", owned.getOrDefault(d.id(), 0));
            out.add(m);
        }
        return out;
    }

    public record BuyResult(boolean ok, String message, int balance) {}

    /** Acquisto: scala i Token e accredita una carica. Atomico (stessa transazione). */
    @Transactional
    public BuyResult buy(String username, String powerId) {
        var defOpt = PowerCatalog.byId(powerId);
        if (defOpt.isEmpty()) {
            return new BuyResult(false, "Potere inesistente", tokens.balance(username));
        }
        PowerCatalog.PowerDef def = defOpt.get();
        int cost = priceOf(def);
        if (!tokens.spend(username, cost)) {
            return new BuyResult(false, "Token insufficienti (servono " + cost + ")", tokens.balance(username));
        }
        inventory.grant(username, powerId, 1);
        return new BuyResult(true, def.label() + " acquistato!", tokens.balance(username));
    }

    // ---- Admin: gestione prezzi ----

    /** Elenco poteri con prezzo corrente e default (per la sezione admin). */
    public List<Map<String, Object>> prices() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (PowerCatalog.PowerDef d : PowerCatalog.POWERS) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.id());
            m.put("game", d.game());
            m.put("label", d.label());
            m.put("emoji", d.emoji());
            m.put("cost", priceOf(d));
            m.put("defaultCost", d.defaultCost());
            out.add(m);
        }
        return out;
    }

    /** Imposta (upsert) il prezzo di un potere. */
    @Transactional
    public boolean setPrice(String powerId, int cost) {
        if (PowerCatalog.byId(powerId).isEmpty()) return false;
        int c = Math.max(0, cost);
        PowerPrice p = PowerPrice.findByPowerId(powerId);
        if (p == null) {
            p = new PowerPrice();
            p.powerId = powerId;
            p.cost = c;
            p.persist();
        } else {
            p.cost = c;
        }
        return true;
    }
}
