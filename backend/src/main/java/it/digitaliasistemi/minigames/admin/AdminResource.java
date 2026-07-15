package it.digitaliasistemi.minigames.admin;

import io.quarkus.elytron.security.common.BcryptUtil;
import it.digitaliasistemi.minigames.daily.DailyHangmanService;
import it.digitaliasistemi.minigames.domain.AppUser;
import it.digitaliasistemi.minigames.domain.DailyAttempt;
import it.digitaliasistemi.minigames.domain.MatchResult;
import it.digitaliasistemi.minigames.domain.Roadmap;
import it.digitaliasistemi.minigames.domain.Announcement;
import it.digitaliasistemi.minigames.domain.DailyRules;
import it.digitaliasistemi.minigames.gift.GiftService;
import it.digitaliasistemi.minigames.shop.CompanionCatalog;
import it.digitaliasistemi.minigames.shop.CompanionService;
import it.digitaliasistemi.minigames.shop.InventoryService;
import it.digitaliasistemi.minigames.shop.PowerCatalog;
import it.digitaliasistemi.minigames.shop.ShopService;
import it.digitaliasistemi.minigames.token.TokenService;
import it.digitaliasistemi.minigames.ws.NotifyBus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/** Funzioni riservate all'admin (role = "admin"). */
@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("admin")
public class AdminResource {

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");

    @Inject DailyHangmanService dailyService;
    @Inject NotifyBus notifyBus;
    @Inject ShopService shop;
    @Inject TokenService tokens;
    @Inject InventoryService inventory;
    @Inject CompanionService companions;
    @Inject GiftService giftService;

    @GET
    @Path("/users")
    public List<UserView> users() {
        return AppUser.<AppUser>listAll().stream()
                .map(u -> new UserView(u.username, u.displayName, u.role))
                .toList();
    }

    /** Azzera lo storico partite dell'utente (stats e leaderboard). */
    @POST
    @Path("/users/{username}/reset-stats")
    @Transactional
    public Response resetStats(@PathParam("username") String username) {
        long deleted = MatchResult.delete("username", username);
        return Response.ok(Map.of("message", "Statistiche azzerate (" + deleted + " partite rimosse)")).build();
    }

    /**
     * Reset della Parola del Giorno per l'utente: gli ridà la lettera e il tentativo parola,
     * e lo riammette se era stato eliminato (oggi).
     */
    @POST
    @Path("/users/{username}/daily-reset")
    @Transactional
    public Response dailyReset(@PathParam("username") String username) {
        DailyAttempt a = DailyAttempt.findByDateAndUser(LocalDate.now(ROME), username);
        if (a == null) {
            return Response.ok(Map.of("message", "Nessun tentativo di oggi per " + username)).build();
        }
        a.letterUsed = false;
        a.wordAttemptUsed = false;
        a.eliminated = false;
        return Response.ok(Map.of("message", "Parola del giorno resettata per " + username)).build();
    }

    /** Imposta una nuova password per l'utente. */
    @POST
    @Path("/users/{username}/reset-password")
    @Transactional
    public Response resetPassword(@PathParam("username") String username, ResetPasswordRequest req) {
        if (req == null || req.newPassword() == null || req.newPassword().length() < 6) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Password troppo corta (min 6)")).build();
        }
        AppUser u = AppUser.findByUsername(username);
        if (u == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("message", "Utente non trovato")).build();
        }
        u.passwordHash = BcryptUtil.bcryptHash(req.newPassword());
        return Response.ok(Map.of("message", "Password aggiornata per " + username)).build();
    }

    /** Imposta parola personalizzata per oggi e resetta la partita. */
    @POST
    @Path("/daily/set-word")
    public Response dailySetWord(DailyWordRequest req) {
        if (req == null || req.word() == null || req.word().isBlank()) {
            return Response.status(400).entity(Map.of("message", "Parola non valida")).build();
        }
        String word = req.word().trim().toLowerCase();
        if (!word.matches("[a-z]+")) {
            return Response.status(400).entity(Map.of("message", "Solo lettere a-z, niente accenti o spazi")).build();
        }
        dailyService.adminSetWord(LocalDate.now(ROME), word);
        notifyBus.broadcast("daily:update");
        return Response.ok(Map.of("message", "Parola impostata: " + word)).build();
    }

    /** Imposta o rimuove (se vuoto) il callout della Parola del Giorno per oggi. */
    @POST
    @Path("/daily/callout")
    public Response dailySetCallout(DailyCalloutRequest req) {
        String msg = req != null ? req.message() : null;
        if (msg != null && msg.length() > 500) {
            return Response.status(400).entity(Map.of("message", "Callout troppo lungo (max 500 caratteri)")).build();
        }
        dailyService.adminSetCallout(LocalDate.now(ROME), msg);
        notifyBus.broadcast("daily:update");
        boolean cleared = msg == null || msg.isBlank();
        return Response.ok(Map.of("message", cleared ? "Callout rimosso" : "Callout impostato")).build();
    }

    /** Reset completo della parola del giorno → riparte con parola automatica. */
    @POST
    @Path("/daily/reset")
    public Response dailyReset() {
        dailyService.adminResetDaily(LocalDate.now(ROME));
        notifyBus.broadcast("daily:update");
        return Response.ok(Map.of("message", "Parola del giorno resettata (parola automatica)")).build();
    }

    /** Aggiorna il testo della roadmap. */
    @PUT
    @Path("/roadmap")
    @Transactional
    public Response setRoadmap(RoadmapRequest req) {
        Roadmap r = Roadmap.getFirst();
        if (r == null) {
            r = new Roadmap();
            r.content = req.content() != null ? req.content() : "";
            r.persist();
        } else {
            r.content = req.content() != null ? req.content() : "";
        }
        return Response.ok(Map.of("message", "Roadmap aggiornata")).build();
    }

    /** Aggiorna il testo della modale "Ultime Fix" (e incrementa la revisione). */
    @PUT
    @Path("/announcement")
    @Transactional
    public Response setAnnouncement(AnnouncementRequest req) {
        String content = req != null && req.content() != null ? req.content() : "";
        Announcement a = Announcement.getFirst();
        if (a == null) {
            a = new Announcement();
            a.content = content;
            a.revision = 1;
            a.persist();
        } else {
            a.content = content;
            a.revision += 1;
        }
        return Response.ok(Map.of("message", "Ultime Fix aggiornate", "revision", a.revision)).build();
    }

    /** Aggiorna il testo delle regole della Parola del Giorno (puro-DB, edit permanente). */
    @PUT
    @Path("/daily-rules")
    @Transactional
    public Response setDailyRules(DailyRulesRequest req) {
        String content = req != null && req.content() != null ? req.content() : "";
        DailyRules r = DailyRules.getFirst();
        if (r == null) {
            r = new DailyRules();
            r.content = content;
            r.persist();
        } else {
            r.content = content;
        }
        return Response.ok(Map.of("message", "Regole della Parola del Giorno aggiornate")).build();
    }

    /** Elenco poteri con prezzo corrente e default (gestione prezzi shop). */
    @GET
    @Path("/power-prices")
    public List<Map<String, Object>> powerPrices() {
        return shop.prices();
    }

    /** Imposta il prezzo (in Token) di un potere. */
    @PUT
    @Path("/power-prices/{id}")
    public Response setPowerPrice(@PathParam("id") String id, PowerPriceRequest req) {
        if (req == null || req.cost() == null || req.cost() < 0) {
            return Response.status(400).entity(Map.of("message", "Prezzo non valido")).build();
        }
        if (!shop.setPrice(id, req.cost())) {
            return Response.status(404).entity(Map.of("message", "Potere inesistente: " + id)).build();
        }
        return Response.ok(Map.of("message", "Prezzo aggiornato (" + req.cost() + " Token)")).build();
    }

    /** Cataloghi regalabili (poteri + companion) per la UI di grant admin. */
    @GET
    @Path("/grantables")
    public Map<String, Object> grantables() {
        List<Map<String, String>> powers = PowerCatalog.POWERS.stream()
                .map(p -> Map.of("id", p.id(), "label", p.label(), "game", p.game())).toList();
        List<Map<String, String>> comps = CompanionCatalog.COMPANIONS.stream()
                .map(c -> Map.of("id", c.id(), "name", c.name())).toList();
        return Map.of("powers", powers, "companions", comps);
    }

    /** Regala all'utente Token, cariche di un potere o un companion (gratis). */
    @POST
    @Path("/users/{username}/grant")
    public Response grant(@PathParam("username") String username, GrantRequest req) {
        if (AppUser.findByUsername(username) == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("message", "Utente non trovato")).build();
        }
        if (req == null || req.type() == null) {
            return Response.status(400).entity(Map.of("message", "Richiesta non valida")).build();
        }
        switch (req.type()) {
            case "tokens" -> {
                int n = req.amount() != null ? req.amount() : 0;
                if (n <= 0) return Response.status(400).entity(Map.of("message", "Quantità non valida")).build();
                tokens.award(username, n);
                giftService.record(username, "tokens", null, "Token", n);
                return Response.ok(Map.of("message", n + " Token accreditati a " + username)).build();
            }
            case "power" -> {
                var def = PowerCatalog.byId(req.id());
                if (def.isEmpty()) return Response.status(400).entity(Map.of("message", "Potere inesistente")).build();
                int n = (req.amount() != null && req.amount() > 0) ? req.amount() : 1;
                inventory.grant(username, req.id(), n);
                giftService.record(username, "power", req.id(), def.get().label(), n);
                return Response.ok(Map.of("message", n + "× " + def.get().label() + " regalati a " + username)).build();
            }
            case "companion" -> {
                var def = CompanionCatalog.byId(req.id());
                if (def.isEmpty()) return Response.status(400).entity(Map.of("message", "Companion inesistente")).build();
                boolean added = companions.grant(username, req.id());
                if (added) giftService.record(username, "companion", req.id(), def.get().name(), 1);
                return Response.ok(Map.of("message", added
                        ? def.get().name() + " regalato a " + username
                        : username + " possiede già " + def.get().name())).build();
            }
            default -> {
                return Response.status(400).entity(Map.of("message", "Tipo non valido: " + req.type())).build();
            }
        }
    }

    public record GrantRequest(String type, String id, Integer amount) {}

    public record UserView(String username, String displayName, String role) {}

    public record PowerPriceRequest(Integer cost) {}

    public record ResetPasswordRequest(String newPassword) {}

    public record DailyWordRequest(String word) {}

    public record DailyCalloutRequest(String message) {}

    public record RoadmapRequest(String content) {}

    public record AnnouncementRequest(String content) {}

    public record DailyRulesRequest(String content) {}
}
