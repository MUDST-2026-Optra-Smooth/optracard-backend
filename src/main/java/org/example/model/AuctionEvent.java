package org.example.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name="auction_events",indexes={@Index(columnList="recipient_id,created_at"),@Index(columnList="auction_id,created_at")}) @Getter @Setter
public class AuctionEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="auction_id",nullable=false) private Integer auctionId;
 private Integer actorId;
 @Column(name="recipient_id") private Integer recipientId;
 @Column(nullable=false,length=40) private String kind;
 @Column(nullable=false,length=40) private String action;
 @Column(columnDefinition="TEXT") private String details;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 private LocalDateTime readAt;
 @Column(unique=true,length=255) private String dedupeKey;
}
