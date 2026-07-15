package it.digitaliasistemi.minigames.gift;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Map;

/** Regali pendenti dell'utente corrente (mostrati in modale al login) e invio doni user→user. */
@Path("/api/gifts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class GiftResource {

    @Inject JsonWebToken jwt;
    @Inject GiftService gifts;
    @Inject GiftSendService sender;

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

    /** Opzioni regalabili verso un destinatario (con flag già-posseduto per i cosmetici). */
    @GET
    @Path("/options")
    public Map<String, Object> options(@QueryParam("to") String to) {
        return sender.options(jwt.getName(), to);
    }

    /** Invia un dono a un altro utente (addebito Token al mittente). */
    @POST
    @Path("/send")
    public Response send(SendGiftRequest req) {
        if (req == null) {
            return Response.status(400).entity(Map.of("message", "Richiesta non valida")).build();
        }
        GiftSendService.SendResult r = sender.send(jwt.getName(), req.toUsername(), req.type(), req.id(), req.amount());
        Map<String, Object> body = Map.of("message", r.message(), "balance", r.balance());
        return r.ok() ? Response.ok(body).build() : Response.status(400).entity(body).build();
    }

    public record SendGiftRequest(String toUsername, String type, String id, Integer amount) {}
}
