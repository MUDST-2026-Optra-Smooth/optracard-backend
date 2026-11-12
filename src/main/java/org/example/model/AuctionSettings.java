package org.example.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="auction_settings") @Getter @Setter
public class AuctionSettings {
 @Id private Integer id=1;
 private Double bidIncrement=50d;
 private Integer checkoutHours=24;
 private Integer antiSnipingMinutes=5;
 private Integer minDurationHours=1;
 private Integer maxDurationHours=168;
 @Version private Long version;
}
