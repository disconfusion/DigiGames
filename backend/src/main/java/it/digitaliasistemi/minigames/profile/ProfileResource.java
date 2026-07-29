package it.digitaliasistemi.minigames.profile;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.Authenticated;
import it.digitaliasistemi.minigames.auth.AuthService;
import it.digitaliasistemi.minigames.auth.dto.AuthResponse;
import it.digitaliasistemi.minigames.domain.AppUser;
import it.digitaliasistemi.minigames.leaderboard.LeaderboardService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;

/** Area personale: lettura profilo, cambio displayName/avatar e password. */
@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class ProfileResource {

    @Inject JsonWebToken jwt;
    @Inject AuthService auth;
    @Inject LeaderboardService leaderboard;
    @Inject it.digitaliasistemi.minigames.shop.CompanionService companions;
    @Inject it.digitaliasistemi.minigames.shop.AccessoryService accessories;
    @Inject it.digitaliasistemi.minigames.house.HouseService houses;

    @GET
    public Response me() {
        AppUser u = AppUser.findByUsername(jwt.getSubject());
        if (u == null) return notFound();
        return Response.ok(new ProfileView(u.username, u.displayName, u.avatar, u.role,
            companions.equippedId(u.username), companions.equippedTint(u.username),
            houses.houseOf(u.username),
            accessories.equipped(u.username))).build();
    }

    @GET
    @Path("/stats")
    public Response stats() {
        return Response.ok(leaderboard.userStats(jwt.getSubject())).build();
    }

    /** Aggiorna displayName e/o avatar. Ritorna un nuovo token con il displayName aggiornato. */
    @PUT
    @Path("/profile")
    @Transactional
    public Response updateProfile(UpdateProfileRequest req) {
        AppUser u = AppUser.findByUsername(jwt.getSubject());
        if (u == null) return notFound();

        if (req != null && req.displayName() != null) {
            String dn = req.displayName().trim();
            if (dn.isBlank() || dn.length() > 40) {
                return error(Response.Status.BAD_REQUEST, "Nome visualizzato non valido (1-40 caratteri)");
            }
            u.displayName = dn;
        }
        if (req != null && req.avatar() != null) {
            String av = req.avatar();
            if (av.length() > 200) {
                return error(Response.Status.BAD_REQUEST, "Avatar non valido");
            }
            u.avatar = av.isBlank() ? null : av;
        }

        String token = auth.issueToken(u.username, u.displayName, u.role);
        return Response.ok(new AuthResponse(token, u.username, u.displayName, u.role)).build();
    }

    @PUT
    @Path("/password")
    @Transactional
    public Response changePassword(ChangePasswordRequest req) {
        if (req == null || req.currentPassword() == null || req.newPassword() == null) {
            return error(Response.Status.BAD_REQUEST, "Dati mancanti");
        }
        if (req.newPassword().length() < 6) {
            return error(Response.Status.BAD_REQUEST, "La nuova password deve avere almeno 6 caratteri");
        }
        AppUser u = AppUser.findByUsername(jwt.getSubject());
        if (u == null) return notFound();
        if (!auth.verifyPassword(req.currentPassword(), u.passwordHash)) {
            return error(Response.Status.UNAUTHORIZED, "Password attuale errata");
        }
        u.passwordHash = BcryptUtil.bcryptHash(req.newPassword());
        return Response.ok(Map.of("message", "Password aggiornata")).build();
    }

    private Response notFound() {
        return error(Response.Status.NOT_FOUND, "Utente non trovato");
    }

    private Response error(Response.Status status, String message) {
        return Response.status(status).entity(Map.of("message", message)).build();
    }

    public record ProfileView(String username, String displayName, String avatar, String role, String companion,
                              String companionTint, String house,
                              java.util.List<it.digitaliasistemi.minigames.shop.AccessoryService.EquippedView> accessories) {}

    public record UpdateProfileRequest(String displayName, String avatar) {}

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 6) String newPassword) {}
}
