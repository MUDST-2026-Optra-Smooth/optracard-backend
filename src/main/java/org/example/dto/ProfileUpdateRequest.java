package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Customer profile update payload")
public record ProfileUpdateRequest(
        @Schema(description = "Updated username", example = "kaiba_ceo")
        String username,

        @Schema(description = "Updated email address", example = "seto.kaiba@kaiba.com")
        String email,

        @Schema(description = "Updated contact phone number", example = "+66899887766")
        String phone,

        @Schema(description = "Updated primary delivery address", example = "456 Obelisk Tower, Sukhumvit, Bangkok 10110")
        String address,

        @Schema(description = "New password (leave null or blank to keep existing password)", example = "NewSecretPassword2026!")
        String password,

        @Schema(description = "Updated profile avatar URL", example = "https://images.optracard.com/avatars/kaiba-updated.jpg")
        String profileImage
) {}
