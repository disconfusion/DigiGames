package it.digitaliasistemi.minigames.admin;

import io.quarkus.elytron.security.common.BcryptUtil;
import it.digitaliasistemi.minigames.domain.AppUser;
import it.digitaliasistemi.minigames.domain.DailyAttempt;
import it.digitaliasistemi.minigames.domain.MatchResult;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Funzioni riservate all'admin (role = "admin"). */
@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("admin")
public class AdminResource {

    @GET
    @Path("/users")
    public List<UserView> users() {
        return AppUser.<AppUser>listAll().stream()
                .map(u -> new UserView(u.username, u.displayName, u.role))
                .toList();
    }

    /** Azzera lo storico partite dell'utente (stats e leaderboard). */
    @POST
    @Path("/users/{username}/reset-stats")
    @Transactional
    public Response resetStats(@PathParam("username") String username) {
        long deleted = MatchResult.delete("username", username);
        return Response.ok(Map.of("message", "Statistiche azzerate (" + deleted + " partite rimosse)")).build();
    }

    /**
     * Reset della Parola del Giorno per l'utente: gli ridà la lettera e il tentativo parola,
     * e lo riammette se era stato eliminato (oggi).
     */
    @POST
    @Path("/users/{username}/daily-reset")
    @Transactional
    public Response dailyReset(@PathParam("username") String username) {
        DailyAttempt a = DailyAttempt.findByDateAndUser(LocalDate.now(), username);
        if (a == null) {
            return Response.ok(Map.of("message", "Nessun tentativo di oggi per " + username)).build();
        }
        a.letterUsed = false;
        a.wordAttemptUsed = false;
        a.eliminated = false;
        return Response.ok(Map.of("message", "Parola del giorno resettata per " + username)).build();
    }

    /** Imposta una nuova password per l'utente. */
    @POST
    @Path("/users/{username}/reset-password")
    @Transactional
    public Response resetPassword(@PathParam("username") String username, ResetPasswordRequest req) {
        if (req == null || req.newPassword() == null || req.newPassword().length() < 6) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Password troppo corta (min 6)")).build();
        }
        AppUser u = AppUser.findByUsername(username);
        if (u == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("message", "Utente non trovato")).build();
        }
        u.passwordHash = BcryptUtil.bcryptHash(req.newPassword());
        return Response.ok(Map.of("message", "Password aggiornata per " + username)).build();
    }

    public record UserView(String username, String displayName, String role) {}

    public record ResetPasswordRequest(String newPassword) {}
}
