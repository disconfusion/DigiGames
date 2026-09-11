package it.digitaliasistemi.minigames.rooms;

import io.quarkus.security.Authenticated;
import it.digitaliasistemi.minigames.game.GameEngine;
import it.digitaliasistemi.minigames.game.GameEngines;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@Path("/api/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class RoomResource {

    @Inject
    RoomManager manager;

    @Inject
    JsonWebToken jwt;

    @Inject
    GameEngines engines;

    @GET
    public List<RoomView> listPublic() {
        return manager.listPublicWaiting().stream().map(RoomView::of).toList();
    }

    /** Stanze in cui l'utente corrente è coinvolto con una partita ancora in corso. */
    @GET
    @Path("/mine")
    public List<RoomView> listMine() {
        return manager.listActiveForUser(jwt.getName()).stream().map(RoomView::of).toList();
    }

    @GET
    @Path("/{code}")
    public Response get(@PathParam("code") String code) {
        Room r = manager.get(code);
        if (r == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(RoomView.of(r)).build();
    }

    @POST
    public Response create(@Valid CreateRoomRequest req) {
        GameEngine engine = engines.get(req.gameSlug());
        if (engine == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMsg("Gioco sconosciuto: " + req.gameSlug())).build();
        }
        Room r = manager.create(req.gameSlug(), jwt.getName(),
                Boolean.TRUE.equals(req.isPrivate()), engine.maxPlayers(req.options()), req.options());
        return Response.status(Response.Status.CREATED).entity(RoomView.of(r)).build();
    }

    public record ErrorMsg(String message) {}

    public record CreateRoomRequest(@NotBlank String gameSlug, Boolean isPrivate,
                                    com.fasterxml.jackson.databind.JsonNode options) {}

    public record RoomView(String code, String gameSlug, String hostEmail, String status,
                           int players, int maxPlayers, boolean isPrivate) {
        static RoomView of(Room r) {
            return new RoomView(r.code, r.gameSlug, r.hostEmail, r.status.name(),
                    r.players.size(), r.maxPlayers, r.isPrivate);
        }
    }
}
