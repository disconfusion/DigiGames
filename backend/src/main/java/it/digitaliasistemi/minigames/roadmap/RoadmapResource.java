package it.digitaliasistemi.minigames.roadmap;

import io.quarkus.security.Authenticated;
import it.digitaliasistemi.minigames.domain.Roadmap;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/** Roadmap pubblica (tutti gli utenti autenticati). */
@Path("/api/roadmap")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class RoadmapResource {

    @GET
    public Response get() {
        Roadmap r = Roadmap.getFirst();
        String content = (r != null && r.content != null) ? r.content : "";
        return Response.ok(Map.of("content", content)).build();
    }
}
