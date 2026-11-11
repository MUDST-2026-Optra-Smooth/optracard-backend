package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credentials for user authentication")
public record AuthRequest(
        @Schema(description = "User email address", example = "seller1@optracard.com")
        String email,

        @Schema(description = "Account password", example = "SellerPassword123!")
        String password
) {}
