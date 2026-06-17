package it.digitaliasistemi.minigames.auth.dto;

public record AuthResponse(String token, String username, String displayName, String role) {}
