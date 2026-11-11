package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** All seller application fields collected by the My Shop wizard. */
@Schema(description = "Merchant store application request payload")
public record StoreApplicationRequest(
        @Schema(description = "Store display name", example = "Siam Card Arena")
        String storeName,

        @Schema(description = "Store description and background", example = "Premier local card shop specializing in One Piece and Pokemon TCG.")
        String storeDescription,

        @Schema(description = "Whether store operates a brick-and-mortar storefront", example = "true")
        Boolean physicalStore,

        @Schema(description = "Owner legal first name", example = "Somchai")
        String ownerFirstName,

        @Schema(description = "Owner legal last name", example = "Prasert")
        String ownerLastName,

        @Schema(description = "Owner contact email", example = "somchai@siamcard.com")
        String ownerEmail,

        @Schema(description = "Owner phone number", example = "+66891234567")
        String ownerPhone,

        @Schema(description = "Settlement bank name", example = "Kasikorn Bank")
        String bankName,

        @Schema(description = "Bank branch name", example = "Siam Paragon")
        String bankBranch,

        @Schema(description = "Name on bank account", example = "Somchai Prasert")
        String bankAccountName,

        @Schema(description = "Bank account number", example = "123-4-56789-0")
        String bankAccountNumber,

        @Schema(description = "Store street address", example = "999 Rama I Road, Pathum Wan")
        String storeAddress,

        @Schema(description = "Province", example = "Bangkok")
        String province,

        @Schema(description = "District", example = "Pathum Wan")
        String district,

        @Schema(description = "Subdistrict", example = "Pathum Wan")
        String subdistrict,

        @Schema(description = "Postal code", example = "10330")
        String postalCode,

        @Schema(description = "Store logo or storefront photo URL", example = "https://images.optracard.com/stores/siamcard.png")
        String storeProfileImage,

        @Schema(description = "Bank passbook document URL for verification", example = "https://images.optracard.com/docs/passbook-101.jpg")
        String bankPassbookImage,

        @Schema(description = "Acceptance of marketplace merchant terms and policies", example = "true")
        Boolean termsAccepted
) {}
