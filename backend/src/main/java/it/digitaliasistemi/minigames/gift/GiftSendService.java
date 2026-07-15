package it.digitaliasistemi.minigames.gift;

import it.digitaliasistemi.minigames.domain.AppUser;
import it.digitaliasistemi.minigames.domain.OwnedAccessory;
import it.digitaliasistemi.minigames.domain.OwnedCompanion;
import it.digitaliasistemi.minigames.shop.AccessoryCatalog;
import it.digitaliasistemi.minigames.shop.AccessoryService;
import it.digitaliasistemi.minigames.shop.CompanionCatalog;
import it.digitaliasistemi.minigames.shop.CompanionService;
import it.digitaliasistemi.minigames.shop.InventoryService;
import it.digitaliasistemi.minigames.shop.PowerCatalog;
import it.digitaliasistemi.minigames.shop.ShopService;
import it.digitaliasistemi.minigames.token.TokenService;
import it.digitaliasistemi.minigames.ws.NotifyBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Invio doni user→user: il mittente paga (Token dal proprio saldo, oppure il prezzo shop del
 * cosmetico/potere "comprato in regalo") e il destinatario riceve l'oggetto come Gift pendente.
 * I cosmetici (companion/accessorio) sono bloccati se il destinatario li possiede già; i
 * consumabili (poteri) e i Token sono sempre inviabili.
 */
@ApplicationScoped
public class GiftSendService {

    @Inject TokenService tokens;
    @Inject InventoryService inventory;
    @Inject CompanionService companions;
    @Inject AccessoryService accessories;
    @Inject ShopService shop;
    @Inject GiftService gifts;
    @Inject NotifyBus notifyBus;

    public record SendResult(boolean ok, String message, int balance) {}

    private SendResult fail(String from, String msg) { return new SendResult(false, msg, tokens.balance(from)); }
    private SendResult ok(String from, String msg) { return new SendResult(true, msg, tokens.balance(from)); }

    private void notify(String to, String fromDisplay) {
        notifyBus.push(to, "gift", Map.of("from", fromDisplay != null ? fromDisplay : ""));
    }

    @Transactional
    public SendResult send(String from, String toRaw, String type, String id, Integer amount) {
        if (toRaw == null || toRaw.isBlank()) return fail(from, "Destinatario mancante");
        String to = toRaw.trim().toLowerCase();
        if (to.equals(from)) return fail(from, "Non puoi regalare a te stesso");
        AppUser dest = AppUser.findByUsername(to);
        if (dest == null) return fail(from, "Destinatario inesistente");
        AppUser sender = AppUser.findByUsername(from);
        String fromDisplay = sender != null ? sender.displayName : from;
        if (type == null) return fail(from, "Tipo dono mancante");

        switch (type) {
            case "tokens" -> {
                int n = amount != null ? amount : 0;
                if (n <= 0) return fail(from, "Quantità Token non valida");
                if (!tokens.spend(from, n)) return fail(from, "Token insufficienti");
                tokens.award(to, n);
                gifts.recordFrom(to, "tokens", null, "Token", n, from, fromDisplay);
                notify(to, fromDisplay);
                return ok(from, n + " Token regalati a " + dest.displayName);
            }
            case "power" -> {
                var defOpt = PowerCatalog.byId(id);
                if (defOpt.isEmpty()) return fail(from, "Potere inesistente");
                var def = defOpt.get();
                int cost = shop.priceOf(def);
                if (!tokens.spend(from, cost)) return fail(from, "Token insufficienti (servono " + cost + ")");
                inventory.grant(to, id, 1);
                gifts.recordFrom(to, "power", id, def.label(), 1, from, fromDisplay);
                notify(to, fromDisplay);
                return ok(from, def.label() + " regalato a " + dest.displayName);
            }
            case "companion" -> {
                var defOpt = CompanionCatalog.byId(id);
                if (defOpt.isEmpty()) return fail(from, "Companion inesistente");
                if (OwnedCompanion.find(to, id) != null) return fail(from, dest.displayName + " possiede già questo companion");
                var def = defOpt.get();
                if (!tokens.spend(from, def.defaultCost())) return fail(from, "Token insufficienti (servono " + def.defaultCost() + ")");
                companions.grant(to, id);
                gifts.recordFrom(to, "companion", id, def.name(), 1, from, fromDisplay);
                notify(to, fromDisplay);
                return ok(from, def.name() + " regalato a " + dest.displayName);
            }
            case "accessory" -> {
                var defOpt = AccessoryCatalog.byId(id);
                if (defOpt.isEmpty()) return fail(from, "Accessorio inesistente");
                if (OwnedAccessory.find(to, id) != null) return fail(from, dest.displayName + " possiede già questo accessorio");
                var def = defOpt.get();
                if (!tokens.spend(from, def.defaultCost())) return fail(from, "Token insufficienti (servono " + def.defaultCost() + ")");
                accessories.grant(to, id);
                gifts.recordFrom(to, "accessory", id, def.name(), 1, from, fromDisplay);
                notify(to, fromDisplay);
                return ok(from, def.name() + " regalato a " + dest.displayName);
            }
            default -> {
                return fail(from, "Tipo dono non valido: " + type);
            }
        }
    }

    /**
     * Opzioni regalabili per un destinatario: saldo del mittente + cataloghi con flag
     * "già posseduto dal destinatario" per i cosmetici (così la UI li disabilita).
     */
    public Map<String, Object> options(String from, String toRaw) {
        String to = toRaw != null ? toRaw.trim().toLowerCase() : "";

        List<Map<String, Object>> comps = new ArrayList<>();
        for (CompanionCatalog.CompanionDef d : CompanionCatalog.COMPANIONS) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.id());
            m.put("name", d.name());
            m.put("cost", d.defaultCost());
            m.put("ownedByRecipient", !to.isBlank() && OwnedCompanion.find(to, d.id()) != null);
            comps.add(m);
        }

        List<Map<String, Object>> accs = new ArrayList<>();
        for (AccessoryCatalog.AccessoryDef d : AccessoryCatalog.ACCESSORIES) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.id());
            m.put("name", d.name());
            m.put("slot", d.slot());
            m.put("cost", d.defaultCost());
            m.put("ownedByRecipient", !to.isBlank() && OwnedAccessory.find(to, d.id()) != null);
            accs.add(m);
        }

        List<Map<String, Object>> pows = new ArrayList<>();
        for (PowerCatalog.PowerDef d : PowerCatalog.POWERS) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.id());
            m.put("label", d.label());
            m.put("game", d.game());
            m.put("cost", shop.priceOf(d));
            pows.add(m);
        }

        return Map.of(
            "balance", tokens.balance(from),
            "companions", comps,
            "accessories", accs,
            "powers", pows
        );
    }
}
