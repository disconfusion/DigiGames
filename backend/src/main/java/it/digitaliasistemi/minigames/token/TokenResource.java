package it.digitaliasistemi.minigames.token;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;

/** Saldo Token dell'utente autenticato. */
@Path("/api/tokens")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class TokenResource {

    @Inject TokenService tokens;
    @Inject JsonWebToken jwt;

    @GET
    public Map<String, Integer> balance() {
        return Map.of("balance", tokens.balance(jwt.getSubject()));
    }
}
