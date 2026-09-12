package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "marketplace_stores")
public class MarketplaceStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Integer storeId;

    /** The Users_Admins record for the customer whose seller application was approved. */
    @Column(name = "seller_user_id", nullable = false)
    private Integer sellerUserId;

    @Column(name = "store_name", nullable = false, length = 255)
    private String storeName;

    @Column(name = "store_slug", nullable = false, unique = true, length = 100)
    private String storeSlug;

    @Column(name = "store_status", nullable = false, length = 30)
    private String storeStatus;

    @Column(name = "store_description", columnDefinition = "TEXT")
    private String storeDescription;

    // Nullable in the JPA mapping so existing databases can be upgraded
    // without failing on old rows; the migration backfills and constrains it.
    @Column(name = "physical_store", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean physicalStore = false;

    @Column(name = "owner_first_name", length = 255)
    private String ownerFirstName;

    @Column(name = "owner_last_name", length = 255)
    private String ownerLastName;

    @Column(name = "owner_email", length = 255)
    private String ownerEmail;

    @Column(name = "owner_phone", length = 50)
    private String ownerPhone;

    @Column(name = "bank_name", length = 255)
    private String bankName;

    @Column(name = "bank_branch", length = 255)
    private String bankBranch;

    @Column(name = "bank_account_name", length = 255)
    private String bankAccountName;

    @Column(name = "bank_account_number", length = 100)
    private String bankAccountNumber;

    @Column(name = "store_address", columnDefinition = "TEXT")
    private String storeAddress;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "subdistrict", length = 100)
    private String subdistrict;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "store_profile_image", columnDefinition = "TEXT")
    private String storeProfileImage;

    @Column(name = "bank_passbook_image", columnDefinition = "TEXT")
    private String bankPassbookImage;

    @Column(name = "terms_accepted", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean termsAccepted = false;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;
}
