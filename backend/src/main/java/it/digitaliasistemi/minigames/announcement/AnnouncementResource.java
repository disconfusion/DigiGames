package it.digitaliasistemi.minigames.announcement;

import io.quarkus.security.Authenticated;
import it.digitaliasistemi.minigames.domain.Announcement;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

/** Modale "Ultime Fix / Novità", visibile a tutti gli utenti autenticati. */
@Path("/api/announcement")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class AnnouncementResource {

    @GET
    public Map<String, Object> get() {
        Announcement a = Announcement.getFirst();
        return Map.of(
            "content", a != null && a.content != null ? a.content : "",
            "revision", a != null ? a.revision : 0
        );
    }
}
