package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after updating user profile")
public record ProfileUpdateResponse(
        @Schema(description = "Updated profile data")
        ProfileResponse profile,

        @Schema(description = "Newly issued JWT Bearer token reflecting updated claims", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Status message", example = "Profile updated successfully")
        String message
) {}
