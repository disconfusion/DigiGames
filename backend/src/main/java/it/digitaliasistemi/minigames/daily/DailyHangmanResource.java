package it.digitaliasistemi.minigames.daily;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDate;
import java.util.Map;

@Path("/api/daily")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class DailyHangmanResource {

    @Inject DailyHangmanService service;
    @Inject JsonWebToken jwt;

    @GET
    public DailyStateDTO get() {
        return service.getState(LocalDate.now(), jwt.getSubject());
    }

    @POST
    @Path("/letter")
    public Response letter(LetterRequest req) {
        if (req == null || req.letter() == null || req.letter().length() != 1) {
            return Response.status(400).entity(Map.of("message", "Lettera non valida")).build();
        }
        char letter = req.letter().toLowerCase().charAt(0);
        if (letter < 'a' || letter > 'z') {
            return Response.status(400).entity(Map.of("message", "Lettera non valida")).build();
        }
        return Response.ok(service.guessLetter(LocalDate.now(), jwt.getSubject(), letter)).build();
    }

    @POST
    @Path("/word")
    public Response word(WordRequest req) {
        if (req == null || req.word() == null || req.word().isBlank()) {
            return Response.status(400).entity(Map.of("message", "Parola non valida")).build();
        }
        return Response.ok(service.guessWord(LocalDate.now(), jwt.getSubject(), req.word())).build();
    }

    public record LetterRequest(String letter) {}
    public record WordRequest(String word) {}
}
