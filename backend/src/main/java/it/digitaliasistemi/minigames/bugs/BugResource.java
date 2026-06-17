package it.digitaliasistemi.minigames.bugs;

import io.quarkus.security.Authenticated;
import it.digitaliasistemi.minigames.domain.AppUser;
import it.digitaliasistemi.minigames.domain.BugReport;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Segnalazioni bug: invio da qualsiasi utente, lettura/cancellazione solo admin. */
@Path("/api/bugs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class BugResource {

    @Inject JsonWebToken jwt;

    @POST
    @Transactional
    public Response create(CreateBugRequest req) {
        if (req == null || req.description() == null || req.description().isBlank()) {
            return error("Descrizione obbligatoria");
        }
        if (req.description().length() > 2000) {
            return error("Descrizione troppo lunga (max 2000 caratteri)");
        }
        AppUser u = AppUser.findByUsername(jwt.getName());
        BugReport b = new BugReport();
        b.username = jwt.getName();
        b.displayName = u != null ? u.displayName : jwt.getName();
        b.game = (req.game() == null || req.game().isBlank()) ? "generale" : req.game();
        b.description = req.description().trim();
        b.createdAt = Instant.now();
        b.persist();
        return Response.status(Response.Status.CREATED).entity(Map.of("message", "Grazie per la segnalazione!")).build();
    }

    @GET
    @RolesAllowed("admin")
    public List<BugView> list() {
        return BugReport.<BugReport>listAll().stream()
                .sorted((a, b) -> b.createdAt.compareTo(a.createdAt))
                .map(b -> new BugView(b.id, b.username, b.displayName, b.game, b.description, b.createdAt.toString()))
                .toList();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = BugReport.deleteById(id);
        if (!deleted) return Response.status(Response.Status.NOT_FOUND).entity(Map.of("message", "Bug non trovato")).build();
        return Response.ok(Map.of("message", "Bug eliminato")).build();
    }

    private Response error(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("message", message)).build();
    }

    public record CreateBugRequest(String game, String description) {}

    public record BugView(Long id, String username, String displayName, String game,
                          String description, String createdAt) {}
}
