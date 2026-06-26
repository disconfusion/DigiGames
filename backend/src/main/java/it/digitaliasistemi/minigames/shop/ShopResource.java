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
}
