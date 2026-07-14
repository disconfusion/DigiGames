package it.digitaliasistemi.minigames.suggestions;

import io.quarkus.security.Authenticated;
import it.digitaliasistemi.minigames.domain.AppUser;
import it.digitaliasistemi.minigames.domain.FeatureSuggestion;
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

/** Suggerimenti feature/gioco: invio da qualsiasi utente, lettura/cancellazione solo admin. */
@Path("/api/suggestions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class SuggestionResource {

    @Inject JsonWebToken jwt;

    @POST
    @Transactional
    public Response create(CreateRequest req) {
        if (req == null || req.description() == null || req.description().isBlank()) {
            return error("Descrizione obbligatoria");
        }
        if (req.description().length() > 2000) {
            return error("Descrizione troppo lunga (max 2000 caratteri)");
        }
        AppUser u = AppUser.findByUsername(jwt.getName());
        FeatureSuggestion s = new FeatureSuggestion();
        s.username = jwt.getName();
        s.displayName = u != null ? u.displayName : jwt.getName();
        s.kind = "game".equalsIgnoreCase(req.kind()) ? "game" : "feature";
        s.description = req.description().trim();
        s.createdAt = Instant.now();
        s.persist();
        return Response.status(Response.Status.CREATED).entity(Map.of("message", "Grazie per il suggerimento!")).build();
    }

    @GET
    @RolesAllowed("admin")
    public List<SuggestionView> list() {
        return FeatureSuggestion.<FeatureSuggestion>listAll().stream()
                .sorted((a, b) -> b.createdAt.compareTo(a.createdAt))
                .map(s -> new SuggestionView(s.id, s.username, s.displayName, s.kind, s.description, s.createdAt.toString()))
                .toList();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = FeatureSuggestion.deleteById(id);
        if (!deleted) return Response.status(Response.Status.NOT_FOUND).entity(Map.of("message", "Suggerimento non trovato")).build();
        return Response.ok(Map.of("message", "Suggerimento eliminato")).build();
    }

    private Response error(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("message", message)).build();
    }

    public record CreateRequest(String kind, String description) {}

    public record SuggestionView(Long id, String username, String displayName, String kind,
                                 String description, String createdAt) {}
}
