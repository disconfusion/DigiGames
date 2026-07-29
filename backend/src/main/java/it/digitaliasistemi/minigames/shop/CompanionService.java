package it.digitaliasistemi.minigames.shop;

import it.digitaliasistemi.minigames.domain.CompanionForm;
import it.digitaliasistemi.minigames.domain.CompanionTint;
import it.digitaliasistemi.minigames.domain.OwnedCompanion;
import it.digitaliasistemi.minigames.token.TokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Logica dei companion cosmetici: catalogo per utente, acquisto, equip. */
@ApplicationScoped
public class CompanionService {

    @Inject TokenService tokens;

    /** Colore valido per un companion? Solo #rrggbb, così finisce sicuro nell'SVG delle icone. */
    private static final java.util.regex.Pattern HEX = java.util.regex.Pattern.compile("^#[0-9a-fA-F]{6}$");

    /** id del companion equipaggiato dall'utente, o null. */
    public String equippedId(String username) {
        OwnedCompanion eq = OwnedCompanion.findEquipped(username);
        return eq != null ? eq.companionId : null;
    }

    /**
     * Id della <b>sprite</b> del companion equipaggiato: l'id nudo, oppure {@code <id>_<forma>}
     * se l'utente lo ha trasformato (es. {@code pipistrello_vampiro}). È quello che le UI
     * disegnano, così header, stanze e classifica mostrano la forma corrente senza saperne nulla.
     */
    public String equippedSpriteId(String username) {
        String id = equippedId(username);
        if (id == null) return null;
        String form = formOf(username, id);
        return CompanionCatalog.BASE_FORM.equals(form) ? id : id + "_" + form;
    }

    /** Forma corrente del companion ("base" se mai trasformato o senza forme alternative). */
    public String formOf(String username, String companionId) {
        CompanionForm f = CompanionForm.find(username, companionId);
        return f != null ? f.form : CompanionCatalog.BASE_FORM;
    }

    /**
     * Cambia la forma di un companion posseduto (es. pipistrello → vampiro).
     * Ritorna false se non posseduto o se la forma non esiste in catalogo.
     */
    @Transactional
    public boolean setForm(String username, String companionId, String form) {
        var def = CompanionCatalog.byId(companionId);
        if (def.isEmpty() || form == null || !def.get().hasForm(form)) return false;
        if (OwnedCompanion.find(username, companionId) == null) return false;
        CompanionForm f = CompanionForm.find(username, companionId);
        if (f == null) {
            f = new CompanionForm();
            f.username = username;
            f.companionId = companionId;
            f.form = form;
            f.persist();
        } else {
            f.form = form; // entità gestita: flush a fine transazione
        }
        return true;
    }

    /**
     * Colore del companion equipaggiato, o null se non è ricolorabile / nessuno equipaggiato.
     * Serve a header, stanze e classifica per disegnare lo stemma nella tinta scelta.
     */
    public String equippedTint(String username) {
        String id = equippedId(username);
        if (id == null) return null;
        return CompanionCatalog.byId(id).filter(CompanionCatalog.CompanionDef::tintable).isPresent()
                ? tintOf(username, id)
                : null;
    }

    /** Colore scelto per quel companion, o il default del catalogo. */
    public String tintOf(String username, String companionId) {
        CompanionTint t = CompanionTint.find(username, companionId);
        return t != null ? t.hex : CompanionCatalog.DEFAULT_TINT;
    }

    /**
     * Imposta il colore di un companion ricolorabile posseduto dall'utente.
     * Ritorna false se non posseduto, non ricolorabile o colore non valido.
     */
    @Transactional
    public boolean setTint(String username, String companionId, String hex) {
        if (hex == null || !HEX.matcher(hex).matches()) return false;
        if (CompanionCatalog.byId(companionId).filter(CompanionCatalog.CompanionDef::tintable).isEmpty()) {
            return false;
        }
        if (OwnedCompanion.find(username, companionId) == null) return false;
        CompanionTint t = CompanionTint.find(username, companionId);
        if (t == null) {
            t = new CompanionTint();
            t.username = username;
            t.companionId = companionId;
            t.hex = hex.toLowerCase();
            t.persist();
        } else {
            t.hex = hex.toLowerCase(); // entità gestita: flush a fine transazione
        }
        return true;
    }

    /** Catalogo completo con flag owned/equipped per l'utente. */
    public List<Map<String, Object>> catalog(String username) {
        Map<String, Boolean> ownedEquip = new HashMap<>();
        for (OwnedCompanion oc : OwnedCompanion.forUser(username)) {
            ownedEquip.put(oc.companionId, oc.equipped);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (CompanionCatalog.CompanionDef d : CompanionCatalog.COMPANIONS) {
            boolean owned = ownedEquip.containsKey(d.id());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.id());
            m.put("name", d.name());
            m.put("description", d.description());
            m.put("cost", d.defaultCost());
            m.put("owned", owned);
            m.put("equipped", owned && Boolean.TRUE.equals(ownedEquip.get(d.id())));
            m.put("tintable", d.tintable());
            if (d.tintable()) m.put("tint", tintOf(username, d.id()));
            m.put("forms", d.forms());
            if (!d.forms().isEmpty()) m.put("form", formOf(username, d.id()));
            out.add(m);
        }
        return out;
    }

    public record BuyResult(boolean ok, String message, int balance) {}

    /** Acquisto: scala i Token e registra il possesso. Auto-equip se non ne hai uno. */
    @Transactional
    public BuyResult buy(String username, String companionId) {
        var defOpt = CompanionCatalog.byId(companionId);
        if (defOpt.isEmpty()) {
            return new BuyResult(false, "Companion inesistente", tokens.balance(username));
        }
        if (OwnedCompanion.find(username, companionId) != null) {
            return new BuyResult(false, "Lo possiedi già", tokens.balance(username));
        }
        CompanionCatalog.CompanionDef def = defOpt.get();
        if (!tokens.spend(username, def.defaultCost())) {
            return new BuyResult(false, "Token insufficienti (servono " + def.defaultCost() + ")", tokens.balance(username));
        }
        boolean firstOne = OwnedCompanion.forUser(username).isEmpty();
        OwnedCompanion oc = new OwnedCompanion();
        oc.username = username;
        oc.companionId = companionId;
        oc.equipped = firstOne; // primo companion: equipaggiato in automatico
        oc.persist();
        return new BuyResult(true, def.name() + " acquistato!", tokens.balance(username));
    }

    /**
     * Regala un companion senza costo (uso admin). Auto-equip se è il primo.
     * Ritorna true se aggiunto, false se id ignoto o già posseduto.
     */
    @Transactional
    public boolean grant(String username, String companionId) {
        if (CompanionCatalog.byId(companionId).isEmpty()) return false;
        if (OwnedCompanion.find(username, companionId) != null) return false;
        boolean firstOne = OwnedCompanion.forUser(username).isEmpty();
        OwnedCompanion oc = new OwnedCompanion();
        oc.username = username;
        oc.companionId = companionId;
        oc.equipped = firstOne;
        oc.persist();
        return true;
    }

    /**
     * Equipaggia il companion indicato (deve essere posseduto). id vuoto o "none"
     * = togli l'equip. Ritorna true se l'operazione è valida.
     */
    @Transactional
    public boolean equip(String username, String companionId) {
        boolean unequip = companionId == null || companionId.isBlank() || companionId.equals("none");
        List<OwnedCompanion> owned = OwnedCompanion.forUser(username);
        if (!unequip && owned.stream().noneMatch(o -> o.companionId.equals(companionId))) {
            return false; // non posseduto
        }
        for (OwnedCompanion oc : owned) {
            oc.equipped = !unequip && oc.companionId.equals(companionId);
        }
        return true;
    }
}
