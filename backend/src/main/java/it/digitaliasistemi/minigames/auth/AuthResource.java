package it.digitaliasistemi.minigames.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import it.digitaliasistemi.minigames.auth.dto.AuthResponse;
import it.digitaliasistemi.minigames.auth.dto.LoginRequest;
import it.digitaliasistemi.minigames.auth.dto.RegisterRequest;
import it.digitaliasistemi.minigames.domain.AppUser;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService auth;

    @POST
    @Path("/register")
    @Transactional
    public Response register(@Valid RegisterRequest req) {
        String username = req.username().trim().toLowerCase();
        if (AppUser.usernameExists(username)) {
            return error(Response.Status.CONFLICT, "Username già registrato");
        }
        AppUser u = new AppUser();
        u.username = username;
        u.displayName = req.displayName().trim();
        u.passwordHash = BcryptUtil.bcryptHash(req.password());
        u.role = "user";
        u.persist();
        String token = auth.issueToken(u.username, u.displayName, u.role);
        return Response.status(Response.Status.CREATED)
                .entity(new AuthResponse(token, u.username, u.displayName)).build();
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest req) {
        String username = req.username().trim().toLowerCase();
        AppUser u = AppUser.findByUsername(username);
        if (u == null || !auth.verifyPassword(req.password(), u.passwordHash)) {
            return error(Response.Status.UNAUTHORIZED, "Credenziali non valide");
        }
        String token = auth.issueToken(u.username, u.displayName, u.role);
        return Response.ok(new AuthResponse(token, u.username, u.displayName)).build();
    }

    private Response error(Response.Status status, String message) {
        return Response.status(status).entity(new ErrorMsg(message)).build();
    }

    public record ErrorMsg(String message) {}
}
