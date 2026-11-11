package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User registration payload")
public record RegisterRequest(
        @Schema(description = "Desired username", example = "kaiba_corp")
        String username,

        @Schema(description = "Unique email address", example = "seto@kaiba.com")
        String email,

        @Schema(description = "Contact phone number", example = "+66812345678")
        String phone,

        @Schema(description = "Primary shipping address", example = "123 Blue-Eyes Blvd, Bangkok 10110")
        String address,

        @Schema(description = "Account password (min 6 characters recommended)", example = "KaibaBlueEyes123!")
        String password
) {}
