package org.example.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "products")
public class Product{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pro_id")
    private Integer proId;

    @Column(name = "game_id")
    private Integer gameId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", insertable = false, updatable = false)
    private CardGame cardGame;

    @Column(name = "pro_name", length = 255)
    private String proName;

    @Column(name = "pro_cost")
    private Double proCost;

    @Column(name = "pro_priceofsell")   
    private Double proPriceOfSell;

    @Column(name = "pro_quantity")
    private Integer proQuantity;

    @Column(name = "pro_type", length = 100)
    private String proType;

    @Column(name = "pro_imageurl", columnDefinition = "TEXT")
    private String proImageUrl;

    @Column(name = "pro_set", length = 255)
    private String productSet;

    @Column(name = "pro_language", length = 100)
    private String language;

    @Column(name = "pro_description", columnDefinition = "TEXT")
    private String proDescription;

    @Column(name = "is_active")
    private Boolean isActive;

    /** OFFICIAL for Optracard-owned stock, MARKETPLACE for an approved seller listing. */
    @Column(name = "pro_listing_source", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'OFFICIAL'")
    private String listingSource = "OFFICIAL";

    /** Marketplace listings stay hidden until an administrator approves them. */
    @Column(name = "pro_approval_status", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'APPROVED'")
    private String approvalStatus = "APPROVED";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private MarketplaceStore store;

    /** Approved Marketplace listing used as the immutable product-information template. */
    @Column(name = "pro_template_id")
    private Integer templateProductId;
}
