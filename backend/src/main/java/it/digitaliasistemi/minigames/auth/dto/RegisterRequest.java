package it.digitaliasistemi.minigames.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Password: minimo 8 caratteri") String password,
        @NotBlank @Size(max = 40) String displayName
) {}
