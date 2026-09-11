package org.example.dto;

public record ProfileUpdateResponse(
        ProfileResponse profile,
        String token,
        String message
) {}
