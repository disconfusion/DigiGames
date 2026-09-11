package it.digitaliasistemi.minigames.social;

import io.quarkus.security.Authenticated;
import it.digitaliasistemi.minigames.domain.AppUser;
import it.digitaliasistemi.minigames.domain.Invitation;
import it.digitaliasistemi.minigames.game.GameEngine;
import it.digitaliasistemi.minigames.game.GameEngines;
import it.digitaliasistemi.minigames.rooms.Room;
import it.digitaliasistemi.minigames.rooms.RoomManager;
import it.digitaliasistemi.minigames.ws.NotifyBus;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Inviti tra giocatori: crea una stanza privata e notifica gli invitati (polling). */
@Path("/api/invites")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class InviteResource {

    @Inject JsonWebToken jwt;
    @Inject RoomManager rooms;
    @Inject GameEngines engines;
    @Inject NotifyBus notifyBus;

    /** Crea una stanza privata e un invito per ciascun utente selezionato. */
    @POST
    @Transactional
    public Response create(CreateInviteRequest req) {
        if (req == null || req.gameSlug() == null || req.usernames() == null || req.usernames().isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "Seleziona un gioco e almeno una persona");
        }
        GameEngine engine = engines.get(req.gameSlug());
        if (engine == null) {
            return error(Response.Status.BAD_REQUEST, "Gioco sconosciuto: " + req.gameSlug());
        }

        String me = jwt.getName();
        AppUser meUser = AppUser.findByUsername(me);
        String myDisplay = meUser != null ? meUser.displayName : me;

        Room room = rooms.create(req.gameSlug(), me, true, engine.maxPlayers(req.options()), req.options());

        Set<String> targets = new LinkedHashSet<>(req.usernames());
        targets.remove(me);
        for (String username : targets) {
            if (!AppUser.usernameExists(username)) continue;
            Invitation inv = new Invitation();
            inv.fromUsername = me;
            inv.fromDisplayName = myDisplay;
            inv.toUsername = username;
            inv.gameSlug = req.gameSlug();
            inv.roomCode = room.code;
            inv.status = "PENDING";
            inv.createdAt = Instant.now();
            inv.persist();
            // inviteId + roomCode nel push: il toast lato client può accettare/rifiutare
            // direttamente, senza passare dalla pagina /invites.
            notifyBus.push(username, "invite", Map.of(
                    "from", myDisplay,
                    "game", req.gameSlug(),
                    "inviteId", inv.id,
                    "roomCode", room.code));
        }
        return Response.status(Response.Status.CREATED)
                .entity(Map.of("roomCode", room.code)).build();
    }

    /** Inviti in attesa per il chiamante. */
    @GET
    public List<InviteView> incoming() {
        return Invitation.pendingFor(jwt.getName()).stream()
                .map(i -> new InviteView(i.id, i.fromDisplayName, i.fromUsername, i.gameSlug,
                        i.roomCode, i.createdAt.toString()))
                .toList();
    }

    @GET
    @Path("/count")
    public Map<String, Long> count() {
        return Map.of("count", Invitation.countPendingFor(jwt.getName()));
    }

    @POST
    @Path("/{id}/accept")
    @Transactional
    public Response accept(@PathParam("id") Long id) {
        Invitation inv = Invitation.findById(id);
        if (inv == null || !inv.toUsername.equals(jwt.getName())) {
            return error(Response.Status.NOT_FOUND, "Invito non trovato");
        }
        inv.status = "ACCEPTED";
        return Response.ok(Map.of("roomCode", inv.roomCode)).build();
    }

    @POST
    @Path("/{id}/decline")
    @Transactional
    public Response decline(@PathParam("id") Long id) {
        Invitation inv = Invitation.findById(id);
        if (inv == null || !inv.toUsername.equals(jwt.getName())) {
            return error(Response.Status.NOT_FOUND, "Invito non trovato");
        }
        inv.status = "DECLINED";
        return Response.ok(Map.of("message", "Invito rifiutato")).build();
    }

    private Response error(Response.Status status, String message) {
        return Response.status(status).entity(Map.of("message", message)).build();
    }

    public record CreateInviteRequest(String gameSlug, List<String> usernames,
                                      com.fasterxml.jackson.databind.JsonNode options) {}

    public record InviteView(Long id, String fromDisplayName, String fromUsername,
                             String gameSlug, String roomCode, String createdAt) {}
}
