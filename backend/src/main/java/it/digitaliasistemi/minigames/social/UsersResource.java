package it.digitaliasistemi.minigames.social;

import io.quarkus.security.Authenticated;
import it.digitaliasistemi.minigames.domain.AppUser;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

/** Elenco degli utenti iscritti (per invitarli a giocare), escluso il chiamante. */
@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class UsersResource {

    @Inject
    JsonWebToken jwt;

    @GET
    public List<UserView> list() {
        String me = jwt.getName();
        return AppUser.<AppUser>listAll().stream()
                .filter(u -> !u.username.equals(me))
                .map(u -> new UserView(u.username, u.displayName, u.avatar))
                .toList();
    }

    public record UserView(String username, String displayName, String avatar) {}
}
