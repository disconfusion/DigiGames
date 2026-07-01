package it.digitaliasistemi.minigames.gift;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Map;

/** Regali pendenti dell'utente corrente (mostrati in modale al login). */
@Path("/api/gifts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class GiftResource {

    @Inject JsonWebToken jwt;
    @Inject GiftService gifts;

    @GET
    public List<Map<String, Object>> mine() {
        return gifts.pending(jwt.getName());
    }

    /** L'utente ha visto la modale: cancella i regali pendenti. */
    @POST
    @Path("/ack")
    public Response ack() {
        gifts.ack(jwt.getName());
        return Response.ok(Map.of("ok", true)).build();
    }
}
