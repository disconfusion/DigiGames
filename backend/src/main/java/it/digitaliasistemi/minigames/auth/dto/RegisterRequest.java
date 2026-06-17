package it.digitaliasistemi.minigames.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "^[a-zA-Z0-9_.-]+$",
                message = "Username: solo lettere, numeri, _ . -") String username,
        @NotBlank @Size(min = 8, message = "Password: minimo 8 caratteri") String password,
        @NotBlank @Size(max = 40) String displayName
) {}
