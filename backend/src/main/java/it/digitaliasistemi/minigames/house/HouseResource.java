package it.digitaliasistemi.minigames.house;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Map;

/** Casate (clan): catalogo, scelta, classifica casate. */
@Path("/api/houses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class HouseResource {

    @Inject HouseService houses;
    @Inject JsonWebToken jwt;

    /** Catalogo casate + casata corrente dell'utente ("" se nessuna). */
    @GET
    public Map<String, Object> list() {
        String mine = houses.houseOf(jwt.getSubject());
        return Map.of(
            "houses", HouseCatalog.HOUSES.stream()
                .map(h -> Map.of("id", h.id(), "name", h.name()))
                .toList(),
            "mine", mine != null ? mine : ""
        );
    }

    /** Classifica casate (somma punti membri). */
    @GET
    @Path("/standings")
    public List<Map<String, Object>> standings() {
        return houses.standings();
    }

    /** Entra (o cambia) nella casata indicata. */
    @POST
    @Path("/{id}/join")
    public Response join(@PathParam("id") String id) {
        if (!houses.setHouse(jwt.getSubject(), id)) {
            return Response.status(400).entity(Map.of("message", "Casata inesistente")).build();
        }
        return Response.ok(Map.of("mine", id)).build();
    }
}
