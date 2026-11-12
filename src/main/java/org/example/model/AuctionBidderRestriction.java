package org.example.model;
import jakarta.persistence.*;import lombok.Getter;import lombok.Setter;
@Entity @Table(name="auction_bidder_restrictions") @Getter @Setter
public class AuctionBidderRestriction { @Id private Integer userId; private boolean banned; @Column(nullable=false,length=1000) private String reason; }
