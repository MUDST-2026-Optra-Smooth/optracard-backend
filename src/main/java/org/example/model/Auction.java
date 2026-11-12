package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** A one-item listing that is sold by competitive bidding instead of the cart. */
@Entity
@Table(name = "auctions")
@Getter
@Setter
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auction_id")
    private Integer auctionId;

    @Column(name = "seller_user_id", nullable = false)
    private Integer sellerUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private MarketplaceStore store;

    @Column(name = "auction_source", nullable = false, length = 20)
    private String source;

    @Column(name = "auction_status", nullable = false, length = 30)
    private String status = AuctionStatus.DRAFT.name();

    @Column(name = "auction_title", nullable = false, length = 255)
    private String title;

    @Column(name = "game_id")
    private Integer gameId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", insertable = false, updatable = false)
    private CardGame cardGame;

    @Column(name = "auction_type", length = 100)
    private String type;

    @Column(name = "auction_image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "auction_description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "auction_starting_price", nullable = false)
    private Double startingPrice;

    @Column(name = "auction_current_price", nullable = false)
    private Double currentPrice;

    @Column(name = "auction_bid_count", nullable = false)
    private Integer bidCount = 0;

    @Column(name = "auction_starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "auction_ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "auction_checkout_deadline")
    private LocalDateTime checkoutDeadline;

    @Column(name = "auction_winning_user_id")
    private Integer winningUserId;

    @Column(name = "auction_review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "auction_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "auction_updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "auction_version")
    private Long version;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }
}
