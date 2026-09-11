package org.example.dto;

public record ProfileUpdateRequest(
        String username,
        String email,
        String phone,
        String address,
        String password
) {}
