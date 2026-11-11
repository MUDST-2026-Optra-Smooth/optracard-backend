package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response containing JWT bearer token and user claims")
public record AuthResponse(
        @Schema(description = "JWT Bearer authentication token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Unique user identifier", example = "101")
        Integer userId,

        @Schema(description = "Account username", example = "kaiba_corp")
        String username,

        @Schema(description = "User email address", example = "seto@kaiba.com")
        String email,

        @Schema(description = "Assigned security role (USER, SELLER, ADMIN, SUPER_ADMIN)", example = "USER")
        String role,

        @Schema(description = "Avatar or profile image URL", example = "https://images.optracard.com/avatars/user-101.jpg")
        String profileImage
) {}
