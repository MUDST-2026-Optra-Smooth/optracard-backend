package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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
}
