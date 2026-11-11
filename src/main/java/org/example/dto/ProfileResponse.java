package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Customer profile representation including purchased card collection")
public record ProfileResponse(
        @Schema(description = "Unique user identifier", example = "101")
        Integer userId,

        @Schema(description = "Account username", example = "kaiba_ceo")
        String username,

        @Schema(description = "Registered email address", example = "seto.kaiba@kaiba.com")
        String email,

        @Schema(description = "Contact phone number", example = "+66899887766")
        String phone,

        @Schema(description = "Primary delivery address", example = "456 Obelisk Tower, Sukhumvit, Bangkok 10110")
        String address,

        @Schema(description = "Avatar or profile photo URL", example = "https://images.optracard.com/avatars/kaiba.jpg")
        String profileImage,

        @Schema(description = "Collection of trading cards acquired through delivered orders")
        List<CollectionItemResponse> collection
) {}
