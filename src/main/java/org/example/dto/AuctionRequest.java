package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Payload for submitting or updating an auction listing")
public record AuctionRequest(
        @Schema(description = "Title of the auction listing", example = "1st Edition Shadowless Charizard Holo")
        String title,

        @Schema(description = "ID of the associated card game", example = "1")
        Integer gameId,

        @Schema(description = "Card type / rarity classification", example = "Single")
        String type,

        @Schema(description = "Card high-resolution photograph or image URL", example = "https://images.optracard.com/auctions/charizard-1st.jpg")
        String imageUrl,

        @Schema(description = "Detailed condition and grading description", example = "PSA 9 Mint condition, stored in UV-protective slab.")
        String description,

        @Schema(description = "Starting bid price in THB", example = "15000.00")
        Double startingPrice,

        @Schema(description = "Minimum amount added to the current bid for this auction, in THB", example = "100.00")
        Double bidIncrement,

        @Schema(description = "Auction start timestamp (ISO-8601)", example = "2026-11-15T12:00:00")
        LocalDateTime startsAt,

        @Schema(description = "Auction closing timestamp (ISO-8601)", example = "2026-11-20T20:00:00")
        LocalDateTime endsAt
, Integer productId, String cardCondition, String grading, java.util.List<String> evidenceImages,
        String productSet, String language
) {
 public AuctionRequest(String title,Integer gameId,String type,String imageUrl,String description,Double startingPrice,LocalDateTime startsAt,LocalDateTime endsAt) {
  this(title,gameId,type,imageUrl,description,startingPrice,null,startsAt,endsAt,null,null,null,java.util.List.of(),null,null);
 }
 public AuctionRequest(String title,Integer gameId,String type,String imageUrl,String description,Double startingPrice,LocalDateTime startsAt,LocalDateTime endsAt,
                       Integer productId,String cardCondition,String grading,java.util.List<String> evidenceImages) {
  this(title,gameId,type,imageUrl,description,startingPrice,null,startsAt,endsAt,productId,cardCondition,grading,evidenceImages,null,null);
 }
 public AuctionRequest(String title,Integer gameId,String type,String imageUrl,String description,Double startingPrice,LocalDateTime startsAt,LocalDateTime endsAt,
                       Integer productId,String cardCondition,String grading,java.util.List<String> evidenceImages,String productSet,String language) {
  this(title,gameId,type,imageUrl,description,startingPrice,null,startsAt,endsAt,productId,cardCondition,grading,evidenceImages,productSet,language);
 }
}
