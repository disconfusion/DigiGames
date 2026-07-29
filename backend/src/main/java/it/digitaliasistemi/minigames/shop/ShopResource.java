package it.digitaliasistemi.minigames.shop;

import io.quarkus.security.Authenticated;
import it.digitaliasistemi.minigames.token.TokenService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;

/** Shop: catalogo poteri con prezzi e quantità possedute, e acquisto. */
@Path("/api/shop")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class ShopResource {

    @Inject ShopService shop;
    @Inject CompanionService companions;
    @Inject AccessoryService accessories;
    @Inject TokenService tokens;
    @Inject JsonWebToken jwt;

    /** Catalogo completo + saldo Token dell'utente. */
    @GET
    @Path("/powers")
    public Map<String, Object> powers() {
        String u = jwt.getSubject();
        return Map.of("balance", tokens.balance(u), "powers", shop.catalog(u));
    }

    /** Acquista una carica del potere indicato. */
    @POST
    @Path("/powers/{id}/buy")
    public Response buy(@PathParam("id") String id) {
        ShopService.BuyResult r = shop.buy(jwt.getSubject(), id);
        Map<String, Object> body = Map.of("message", r.message(), "balance", r.balance());
        return r.ok() ? Response.ok(body).build() : Response.status(400).entity(body).build();
    }

    // ---- Companion (cosmetici) ----

    /** Catalogo companion + saldo + id equipaggiato ("" se nessuno). */
    @GET
    @Path("/companions")
    public Map<String, Object> companions() {
        String u = jwt.getSubject();
        String eq = companions.equippedId(u);
        return Map.of(
            "balance", tokens.balance(u),
            "companions", companions.catalog(u),
            "equipped", eq != null ? eq : ""
        );
    }

    /** Acquista il companion indicato. */
    @POST
    @Path("/companions/{id}/buy")
    public Response buyCompanion(@PathParam("id") String id) {
        CompanionService.BuyResult r = companions.buy(jwt.getSubject(), id);
        Map<String, Object> body = Map.of("message", r.message(), "balance", r.balance());
        return r.ok() ? Response.ok(body).build() : Response.status(400).entity(body).build();
    }

    /** Equipaggia il companion ("none" per toglierlo). */
    @POST
    @Path("/companions/{id}/equip")
    public Response equipCompanion(@PathParam("id") String id) {
        boolean ok = companions.equip(jwt.getSubject(), id);
        if (!ok) {
            return Response.status(400).entity(Map.of("message", "Companion non posseduto")).build();
        }
        String eq = companions.equippedId(jwt.getSubject());
        return Response.ok(Map.of("equipped", eq != null ? eq : "")).build();
    }

    /** Imposta il colore di un companion ricolorabile (es. scarabeo). */
    @POST
    @Path("/companions/{id}/tint")
    public Response tintCompanion(@PathParam("id") String id, TintRequest req) {
        String hex = req != null ? req.hex() : null;
        if (!companions.setTint(jwt.getSubject(), id, hex)) {
            return Response.status(400)
                    .entity(Map.of("message", "Colore non valido o companion non ricolorabile")).build();
        }
        return Response.ok(Map.of("tint", companions.tintOf(jwt.getSubject(), id))).build();
    }

    public record TintRequest(String hex) {}

    /** Cambia la forma di un companion che ne prevede più di una (pipistrello → vampiro). */
    @POST
    @Path("/companions/{id}/form")
    public Response formCompanion(@PathParam("id") String id, FormRequest req) {
        String form = req != null ? req.form() : null;
        if (!companions.setForm(jwt.getSubject(), id, form)) {
            return Response.status(400)
                    .entity(Map.of("message", "Forma non valida o companion non posseduto")).build();
        }
        return Response.ok(Map.of("form", companions.formOf(jwt.getSubject(), id))).build();
    }

    public record FormRequest(String form) {}

    // ---- Accessori avatar (cosmetici, multi-slot) ----

    /** Catalogo accessori + saldo + accessori equipaggiati (id+slot). */
    @GET
    @Path("/accessories")
    public Map<String, Object> accessories() {
        String u = jwt.getSubject();
        return Map.of(
            "balance", tokens.balance(u),
            "accessories", accessories.catalog(u),
            "equipped", accessories.equipped(u)
        );
    }

    /** Acquista l'accessorio indicato. */
    @POST
    @Path("/accessories/{id}/buy")
    public Response buyAccessory(@PathParam("id") String id) {
        AccessoryService.BuyResult r = accessories.buy(jwt.getSubject(), id);
        Map<String, Object> body = Map.of("message", r.message(), "balance", r.balance());
        return r.ok() ? Response.ok(body).build() : Response.status(400).entity(body).build();
    }

    /** Equipaggia/toglie l'accessorio (toggle sullo slot). */
    @POST
    @Path("/accessories/{id}/equip")
    public Response equipAccessory(@PathParam("id") String id) {
        boolean ok = accessories.equip(jwt.getSubject(), id);
        if (!ok) {
            return Response.status(400).entity(Map.of("message", "Accessorio non posseduto")).build();
        }
        return Response.ok(Map.of("equipped", accessories.equipped(jwt.getSubject()))).build();
    }
}
