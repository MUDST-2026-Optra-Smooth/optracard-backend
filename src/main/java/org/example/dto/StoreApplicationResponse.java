package org.example.dto;

import java.time.LocalDateTime;

public record StoreApplicationResponse(
        Integer storeId,
        String storeSlug,
        String storeStatus,
        String storeName,
        String storeDescription,
        Boolean physicalStore,
        String ownerFirstName,
        String ownerLastName,
        String ownerEmail,
        String ownerPhone,
        String bankName,
        String bankBranch,
        String bankAccountName,
        String bankAccountNumber,
        String storeAddress,
        String province,
        String district,
        String subdistrict,
        String postalCode,
        String storeProfileImage,
        String bankPassbookImage,
        Boolean termsAccepted,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        String reviewNote
) {}
