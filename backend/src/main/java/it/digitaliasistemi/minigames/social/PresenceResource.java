package it.digitaliasistemi.minigames.social;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;

/** Heartbeat di presenza: il client lo chiama periodicamente per risultare online. */
@Path("/api/presence")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class PresenceResource {

    @Inject JsonWebToken jwt;
    @Inject PresenceService presence;

    @POST
    @Path("/ping")
    public Map<String, Object> ping() {
        presence.touch(jwt.getName());
        return Map.of("ok", true);
    }
}
