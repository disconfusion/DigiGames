package it.digitaliasistemi.minigames.daily;

import io.quarkus.security.Authenticated;
import it.digitaliasistemi.minigames.domain.DailyRules;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/** Regole della Parola del Giorno, visibili a tutti gli utenti autenticati. */
@Path("/api/daily-rules")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class DailyRulesResource {

    @GET
    public Response get() {
        DailyRules r = DailyRules.getFirst();
        String content = (r != null && r.content != null) ? r.content : "";
        return Response.ok(Map.of("content", content)).build();
    }
}
