package org.example.dto;

/** All seller application fields collected by the My Shop wizard. */
public record StoreApplicationRequest(
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
        Boolean termsAccepted
) {}
