package org.example.service;

import org.example.dto.AuctionResponse;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.Objects;
import java.util.List;

/** Public responses reveal only the viewer's own bidder identity. */
@Service
public class AuctionPrivacyService {
    private final UserRepository users;
    public AuctionPrivacyService(UserRepository users) { this.users = users; }
    public AuctionResponse forViewer(AuctionResponse a, String email) {
        User viewer = email == null ? null : users.findByEmail(email).orElse(null);
        Integer id = viewer == null ? null : viewer.getUaId();
        String role = viewer == null || viewer.getRole() == null ? "" : viewer.getRole().replace("ROLE_", "");
        boolean related = id != null && (Objects.equals(id,a.sellerUserId()) || List.of("ADMIN","SUPER_ADMIN").contains(role));
        Integer winner = related || (id != null && Objects.equals(id,a.winningUserId())) ? a.winningUserId() : null;
        Integer leader = related || (id != null && Objects.equals(id,a.leadingUserId())) ? a.leadingUserId() : null;
        return new AuctionResponse(a.id(),a.title(),a.game(),a.type(),a.imageUrl(),a.description(),
            a.startingPrice(),a.currentPrice(),a.bidCount(),a.startsAt(),a.endsAt(),
            related || winner != null ? a.checkoutDeadline() : null,a.status(),a.source(),a.storeId(),a.storeName(),
            a.sellerUserId(),winner,leader,related ? a.reviewNote() : null,a.createdAt(),a.bidIncrement(),a.checkoutHours(),a.antiSnipingMinutes(),a.productId(),a.cardCondition(),a.grading(),a.evidenceImages(),a.productSet(),a.language());
    }
}
