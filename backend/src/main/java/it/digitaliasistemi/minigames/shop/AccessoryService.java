package it.digitaliasistemi.minigames.shop;

import it.digitaliasistemi.minigames.domain.OwnedAccessory;
import it.digitaliasistemi.minigames.token.TokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Logica degli accessori avatar (cosmetici): catalogo per utente, acquisto, equip multi-slot. */
@ApplicationScoped
public class AccessoryService {

    @Inject TokenService tokens;

    /** Accessorio equipaggiato per ogni slot (id + slot). */
    public record EquippedView(String id, String slot) {}

    /** Lista degli accessori equipaggiati dall'utente (uno per slot). */
    public List<EquippedView> equipped(String username) {
        return OwnedAccessory.findEquipped(username).stream()
            .map(o -> new EquippedView(o.accessoryId, o.slot))
            .toList();
    }

    /** Catalogo completo con flag owned/equipped per l'utente. */
    public List<Map<String, Object>> catalog(String username) {
        Map<String, Boolean> ownedEquip = new HashMap<>();
        for (OwnedAccessory oa : OwnedAccessory.forUser(username)) {
            ownedEquip.put(oa.accessoryId, oa.equipped);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (AccessoryCatalog.AccessoryDef d : AccessoryCatalog.ACCESSORIES) {
            boolean owned = ownedEquip.containsKey(d.id());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.id());
            m.put("name", d.name());
            m.put("description", d.description());
            m.put("slot", d.slot());
            m.put("cost", d.defaultCost());
            m.put("owned", owned);
            m.put("equipped", owned && Boolean.TRUE.equals(ownedEquip.get(d.id())));
            out.add(m);
        }
        return out;
    }

    public record BuyResult(boolean ok, String message, int balance) {}

    /** Acquisto: scala i Token e registra il possesso. Auto-equip se lo slot è libero. */
    @Transactional
    public BuyResult buy(String username, String accessoryId) {
        var defOpt = AccessoryCatalog.byId(accessoryId);
        if (defOpt.isEmpty()) {
            return new BuyResult(false, "Accessorio inesistente", tokens.balance(username));
        }
        if (OwnedAccessory.find(username, accessoryId) != null) {
            return new BuyResult(false, "Lo possiedi già", tokens.balance(username));
        }
        AccessoryCatalog.AccessoryDef def = defOpt.get();
        if (!tokens.spend(username, def.defaultCost())) {
            return new BuyResult(false, "Token insufficienti (servono " + def.defaultCost() + ")", tokens.balance(username));
        }
        boolean slotFree = OwnedAccessory.forUser(username).stream()
            .noneMatch(o -> o.slot.equals(def.slot()) && o.equipped);
        persistOwned(username, def, slotFree);
        return new BuyResult(true, def.name() + " acquistato!", tokens.balance(username));
    }

    /**
     * Regala un accessorio senza costo (uso admin/dono). Auto-equip se lo slot è libero.
     * Ritorna true se aggiunto, false se id ignoto o già posseduto.
     */
    @Transactional
    public boolean grant(String username, String accessoryId) {
        var defOpt = AccessoryCatalog.byId(accessoryId);
        if (defOpt.isEmpty()) return false;
        if (OwnedAccessory.find(username, accessoryId) != null) return false;
        AccessoryCatalog.AccessoryDef def = defOpt.get();
        boolean slotFree = OwnedAccessory.forUser(username).stream()
            .noneMatch(o -> o.slot.equals(def.slot()) && o.equipped);
        persistOwned(username, def, slotFree);
        return true;
    }

    private void persistOwned(String username, AccessoryCatalog.AccessoryDef def, boolean equip) {
        OwnedAccessory oa = new OwnedAccessory();
        oa.username = username;
        oa.accessoryId = def.id();
        oa.slot = def.slot();
        oa.equipped = equip;
        oa.persist();
    }

    /**
     * Equip toggle: se l'accessorio (posseduto) è già equipaggiato lo toglie, altrimenti lo
     * equipaggia togliendo l'eventuale altro accessorio dello stesso slot. Ritorna true se valido.
     */
    @Transactional
    public boolean equip(String username, String accessoryId) {
        List<OwnedAccessory> owned = OwnedAccessory.forUser(username);
        OwnedAccessory target = owned.stream()
            .filter(o -> o.accessoryId.equals(accessoryId)).findFirst().orElse(null);
        if (target == null) return false; // non posseduto
        if (target.equipped) {
            target.equipped = false; // toggle off
            return true;
        }
        for (OwnedAccessory o : owned) {
            if (o.slot.equals(target.slot)) o.equipped = false; // libera lo slot
        }
        target.equipped = true;
        return true;
    }
}
