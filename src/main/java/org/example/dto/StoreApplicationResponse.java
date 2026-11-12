package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Seller store application details and review status")
public record StoreApplicationResponse(
        @Schema(description = "Store ID", example = "5")
        Integer storeId,

        @Schema(description = "Unique store slug", example = "siam-card-arena")
        String storeSlug,

        @Schema(description = "Application status (PENDING, APPROVED, REJECTED)", example = "APPROVED")
        String storeStatus,

        @Schema(description = "Store display name", example = "Siam Card Arena")
        String storeName,

        @Schema(description = "Store description", example = "Premier local card shop specializing in One Piece and Pokemon TCG.")
        String storeDescription,

        @Schema(description = "Physical storefront indicator", example = "true")
        Boolean physicalStore,

        @Schema(description = "Owner first name", example = "Somchai")
        String ownerFirstName,

        @Schema(description = "Owner last name", example = "Prasert")
        String ownerLastName,

        @Schema(description = "Owner email", example = "somchai@siamcard.com")
        String ownerEmail,

        @Schema(description = "Owner phone", example = "+66891234567")
        String ownerPhone,

        @Schema(description = "Bank name", example = "Kasikorn Bank")
        String bankName,

        @Schema(description = "Bank branch", example = "Siam Paragon")
        String bankBranch,

        @Schema(description = "Account holder name", example = "Somchai Prasert")
        String bankAccountName,

        @Schema(description = "Bank account number", example = "123-4-56789-0")
        String bankAccountNumber,

        @Schema(description = "Store address", example = "999 Rama I Road, Pathum Wan")
        String storeAddress,

        @Schema(description = "Province", example = "Bangkok")
        String province,

        @Schema(description = "District", example = "Pathum Wan")
        String district,

        @Schema(description = "Subdistrict", example = "Pathum Wan")
        String subdistrict,

        @Schema(description = "Postal code", example = "10330")
        String postalCode,

        @Schema(description = "Store profile image URL", example = "https://images.optracard.com/stores/siamcard.png")
        String storeProfileImage,

        @Schema(description = "Bank passbook document URL", example = "https://images.optracard.com/docs/passbook-101.jpg")
        String bankPassbookImage,

        @Schema(description = "Terms acceptance flag", example = "true")
        Boolean termsAccepted,

        @Schema(description = "Application submission timestamp", example = "2026-11-01T10:00:00")
        LocalDateTime submittedAt,

        @Schema(description = "Administrator review timestamp", example = "2026-11-02T14:30:00")
        LocalDateTime reviewedAt,

        @Schema(description = "Review comments or rejection feedback", example = "Documents verified")
        String reviewNote
) {}
