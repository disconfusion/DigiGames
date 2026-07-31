package it.digitaliasistemi.minigames.game.battlecity;

import io.quarkus.security.Authenticated;
import it.digitaliasistemi.minigames.domain.AppUser;
import it.digitaliasistemi.minigames.domain.BattleCityMap;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Libreria delle mappe di Battle City disegnate dai giocatori: elenco, salvataggio, rimozione. */
@Path("/api/battlecity/maps")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class BattleCityMapResource {

    /** Tetto alle mappe salvate: il DB di produzione ha spazio limitato (vedi CLAUDE.md). */
    private static final int MAX_MAPS = 200;

    @Inject JsonWebToken jwt;

    @GET
    public List<Map<String, Object>> list() {
        return BattleCityMap.<BattleCityMap>newest().stream().map(BattleCityMapResource::view).toList();
    }

    @POST
    @Transactional
    public Response save(SaveRequest req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            return error(400, "Serve un nome per la mappa");
        }
        String[] rows = BattleCityBuild.sanitize(req.rows());
        if (!BattleCityBuild.isPlayable(rows)) {
            return error(400, "Mappa non giocabile: dai punti di comparsa non si raggiunge la base");
        }
        if (BattleCityMap.count() >= MAX_MAPS) {
            return error(409, "Libreria piena (" + MAX_MAPS + " mappe): cancellane qualcuna");
        }
        String me = jwt.getName();
        AppUser user = AppUser.findByUsername(me);

        BattleCityMap m = new BattleCityMap();
        m.name = req.name().trim().substring(0, Math.min(60, req.name().trim().length()));
        m.authorUsername = me;
        m.authorDisplayName = user != null ? user.displayName : me;
        m.builders = req.builders() != null && !req.builders().isEmpty()
                ? String.join(",", req.builders())
                : me;
        m.setRowArray(rows);
        m.persist();
        return Response.status(Response.Status.CREATED).entity(view(m)).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        BattleCityMap m = BattleCityMap.findById(id);
        if (m == null) return error(404, "Mappa non trovata");
        boolean admin = jwt.getGroups().contains("admin");
        if (!admin && !m.authorUsername.equals(jwt.getName())) {
            return error(403, "Puoi cancellare solo le mappe che hai salvato tu");
        }
        m.delete();
        return Response.ok(Map.of("message", "Mappa eliminata")).build();
    }

    static Map<String, Object> view(BattleCityMap m) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", m.id);
        v.put("name", m.name);
        v.put("author", m.authorDisplayName);
        v.put("authorUsername", m.authorUsername);
        v.put("builders", m.builders == null ? List.of() : List.of(m.builders.split(",")));
        v.put("rows", List.of(m.rowArray()));
        v.put("plays", m.plays);
        v.put("createdAt", m.createdAt.toString());
        return v;
    }

    private Response error(int status, String message) {
        return Response.status(status).entity(Map.of("message", message)).build();
    }

    public record SaveRequest(String name, String[] rows, List<String> builders) {}
}
