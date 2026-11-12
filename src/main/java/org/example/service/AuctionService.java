package org.example.service;

import org.example.dto.AuctionBidRequest;
import org.example.dto.AuctionBidResponse;
import org.example.dto.AuctionCheckoutRequest;
import org.example.dto.AuctionRequest;
import org.example.dto.AuctionResponse;
import org.example.model.Auction;
import org.example.model.AuctionBid;
import org.example.model.AuctionStatus;
import org.example.model.CardGame;
import org.example.model.MarketplaceStore;
import org.example.model.Order;
import org.example.model.User;
import org.example.repository.AuctionBidRepository;
import org.example.repository.AuctionRepository;
import org.example.repository.CardGameRepository;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.OrderRepository;
import org.example.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Keeps the first auction version deliberately small: one listing equals one item,
 * bids are direct/manual, and auction checkout never touches a user's cart.
 */
@Service
public class AuctionService {
    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    private static final double MINIMUM_INCREMENT = 50d;
    private static final double STANDARD_DELIVERY_FEE = 50d;
    private static final Duration MIN_DURATION = Duration.ofHours(1);
    private static final Duration MAX_DURATION = Duration.ofDays(7);
    private static final Duration CHECKOUT_WINDOW = Duration.ofHours(24);

    private final AuctionRepository auctionRepository;
    private final AuctionBidRepository bidRepository;
    private final MarketplaceStoreRepository storeRepository;
    private final CardGameRepository cardGameRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public AuctionService(AuctionRepository auctionRepository,
                          AuctionBidRepository bidRepository,
                          MarketplaceStoreRepository storeRepository,
                          CardGameRepository cardGameRepository,
                          UserRepository userRepository,
                          OrderRepository orderRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.storeRepository = storeRepository;
        this.cardGameRepository = cardGameRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<AuctionResponse> listPublic(String game, String source, String status) {
        return auctionRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(this::isPubliclyVisible)
                .filter(auction -> matches(auction.getCardGame() == null ? null : auction.getCardGame().getGameName(), game))
                .filter(auction -> matches(auction.getSource(), source))
                .filter(auction -> matches(auction.getStatus(), status))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuctionResponse getPublic(Integer auctionId) {
        Auction auction = findAuction(auctionId);
        if (!isPubliclyVisible(auction)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found");
        }
        return toResponse(auction);
    }

    @Transactional(readOnly = true)
    public List<AuctionBidResponse> bidHistory(Integer auctionId) {
        Auction auction = findAuction(auctionId);
        if (!isPubliclyVisible(auction)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found");
        }
        return bidRepository.findByAuction_AuctionIdOrderByCreatedAtDesc(auction.getAuctionId()).stream()
                .map(this::toBidResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AuctionResponse> sellerAuctions(String email) {
        User user = currentUser(email);
        return auctionRepository.findBySellerUserIdOrderByCreatedAtDesc(user.getUaId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public AuctionResponse createSellerAuction(String email, AuctionRequest request) {
        User seller = currentUser(email);
        requireRole(seller, "SELLER");
        MarketplaceStore store = requireApprovedStore(seller.getUaId());
        Auction auction = new Auction();
        auction.setSellerUserId(seller.getUaId());
        auction.setStore(store);
        auction.setSource("MARKETPLACE");
        auction.setStatus(AuctionStatus.DRAFT.name());
        apply(auction, request);
        return toResponse(auctionRepository.saveAndFlush(auction));
    }

    @Transactional
    public AuctionResponse updateSellerAuction(String email, Integer auctionId, AuctionRequest request) {
        User seller = currentUser(email);
        requireRole(seller, "SELLER");
        Auction auction = requireOwnedAuction(seller, auctionId);
        AuctionStatus status = statusOf(auction);
        if (!List.of(AuctionStatus.DRAFT, AuctionStatus.PENDING_REVIEW, AuctionStatus.REJECTED, AuctionStatus.LIVE).contains(status)
                || (status == AuctionStatus.LIVE && auction.getBidCount() > 0)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This auction can no longer be edited because bidding has started or it has ended");
        }
        apply(auction, request);
        // Any edit after the seller submitted the listing needs another review.
        if (status != AuctionStatus.DRAFT) {
            auction.setStatus(AuctionStatus.PENDING_REVIEW.name());
            auction.setReviewNote(null);
        }
        return toResponse(auctionRepository.saveAndFlush(auction));
    }

    @Transactional
    public AuctionResponse submitSellerAuction(String email, Integer auctionId) {
        User seller = currentUser(email);
        requireRole(seller, "SELLER");
        Auction auction = requireOwnedAuction(seller, auctionId);
        if (!List.of(AuctionStatus.DRAFT, AuctionStatus.REJECTED).contains(statusOf(auction))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a draft or rejected auction can be submitted");
        }
        validateTiming(auction.getStartsAt(), auction.getEndsAt());
        auction.setStatus(AuctionStatus.PENDING_REVIEW.name());
        auction.setReviewNote(null);
        return toResponse(auctionRepository.saveAndFlush(auction));
    }

    @Transactional
    public AuctionResponse cancelSellerAuction(String email, Integer auctionId) {
        User seller = currentUser(email);
        requireRole(seller, "SELLER");
        Auction auction = requireOwnedAuction(seller, auctionId);
        AuctionStatus status = statusOf(auction);
        if (!(status == AuctionStatus.DRAFT || status == AuctionStatus.PENDING_REVIEW
                || (status == AuctionStatus.LIVE && auction.getBidCount() == 0))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This auction can no longer be cancelled");
        }
        auction.setStatus(AuctionStatus.CANCELLED.name());
        auction.setUpdatedAt(now());
        return toResponse(auctionRepository.saveAndFlush(auction));
    }

    @Transactional
    public AuctionResponse relistSellerAuction(String email, Integer auctionId, AuctionRequest request) {
        User seller = currentUser(email);
        requireRole(seller, "SELLER");
        Auction previous = requireOwnedAuction(seller, auctionId);
        if (!List.of(AuctionStatus.ENDED_NO_BIDS, AuctionStatus.CHECKOUT_EXPIRED, AuctionStatus.CLOSED).contains(statusOf(previous))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only an ended auction can be relisted");
        }
        Auction replacement = new Auction();
        replacement.setSellerUserId(previous.getSellerUserId());
        replacement.setStore(previous.getStore());
        replacement.setSource(previous.getSource());
        replacement.setStatus(AuctionStatus.DRAFT.name());
        apply(replacement, request);
        return toResponse(auctionRepository.saveAndFlush(replacement));
    }

    @Transactional(readOnly = true)
    public List<AuctionResponse> adminAuctions(String status) {
        return auctionRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(auction -> matches(auction.getStatus(), status))
                .map(this::toResponse).toList();
    }

    @Transactional
    public AuctionResponse createOfficialAuction(String email, AuctionRequest request) {
        User admin = currentUser(email);
        requireAdmin(admin);
        Auction auction = new Auction();
        auction.setSellerUserId(admin.getUaId());
        auction.setSource("OFFICIAL");
        apply(auction, request);
        validateTiming(auction.getStartsAt(), auction.getEndsAt());
        auction.setStatus(auction.getStartsAt().isAfter(now()) ? AuctionStatus.SCHEDULED.name() : AuctionStatus.LIVE.name());
        return toResponse(auctionRepository.saveAndFlush(auction));
    }

    @Transactional
    public AuctionResponse reviewAuction(String email, Integer auctionId, boolean approved, String note) {
        User admin = currentUser(email);
        requireAdmin(admin);
        Auction auction = findAuction(auctionId);
        if (!"MARKETPLACE".equalsIgnoreCase(auction.getSource()) || statusOf(auction) != AuctionStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This marketplace auction is not waiting for review");
        }
        if (!approved) {
            auction.setStatus(AuctionStatus.REJECTED.name());
            auction.setReviewNote(blankToNull(note));
        } else {
            validateTiming(auction.getStartsAt(), auction.getEndsAt());
            auction.setStatus(auction.getStartsAt().isAfter(now()) ? AuctionStatus.SCHEDULED.name() : AuctionStatus.LIVE.name());
            auction.setReviewNote(null);
        }
        auction.setUpdatedAt(now());
        return toResponse(auctionRepository.saveAndFlush(auction));
    }

    @Transactional
    public AuctionResponse cancelByAdmin(String email, Integer auctionId, String note) {
        User admin = currentUser(email);
        requireAdmin(admin);
        Auction auction = findAuction(auctionId);
        if (List.of(AuctionStatus.AWAITING_CHECKOUT, AuctionStatus.COMPLETED).contains(statusOf(auction))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An auction cannot be cancelled after a winner is selected");
        }
        if (auction.getBidCount() > 0 && statusOf(auction) == AuctionStatus.LIVE) {
            // Admin moderation is intentionally allowed before checkout, even after bids.
            auction.setReviewNote(require(note, "A cancellation reason is required"));
        } else {
            auction.setReviewNote(blankToNull(note));
        }
        auction.setStatus(AuctionStatus.CANCELLED.name());
        auction.setUpdatedAt(now());
        return toResponse(auctionRepository.saveAndFlush(auction));
    }

    @Transactional
    public AuctionBidResponse placeBid(String email, Integer auctionId, AuctionBidRequest request) {
        User bidder = currentUser(email);
        requireBidder(bidder);
        if (request == null || request.amount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A bid amount is required");
        }
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
        advanceOne(auction, now());
        if (statusOf(auction) != AuctionStatus.LIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This auction is not accepting bids");
        }
        if (Objects.equals(auction.getSellerUserId(), bidder.getUaId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot bid on your own auction");
        }
        double required = auction.getBidCount() == 0 ? auction.getStartingPrice() : auction.getCurrentPrice() + MINIMUM_INCREMENT;
        if (request.amount() < required) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.US, "Your bid must be at least %.2f", required));
        }
        LocalDateTime bidTime = now();
        AuctionBid bid = new AuctionBid();
        bid.setAuction(auction);
        bid.setBidderUserId(bidder.getUaId());
        bid.setAmount(roundMoney(request.amount()));
        bid.setBidderPublicCode(publicBidderCode(auction.getAuctionId(), bidder.getUaId()));
        bid.setCreatedAt(bidTime);
        bidRepository.save(bid);
        auction.setCurrentPrice(bid.getAmount());
        auction.setBidCount(auction.getBidCount() + 1);
        if (!bidTime.isBefore(auction.getEndsAt().minusMinutes(5))) {
            auction.setEndsAt(auction.getEndsAt().plusMinutes(5));
        }
        auction.setUpdatedAt(bidTime);
        auctionRepository.saveAndFlush(auction);
        return toBidResponse(bid);
    }

    @Transactional
    public AuctionResponse checkout(String email, Integer auctionId, AuctionCheckoutRequest request) {
        User winner = currentUser(email);
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
        advanceOne(auction, now());
        if (statusOf(auction) != AuctionStatus.AWAITING_CHECKOUT || !Objects.equals(auction.getWinningUserId(), winner.getUaId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the auction winner can check out this item");
        }
        if (auction.getCheckoutDeadline() == null || !now().isBefore(auction.getCheckoutDeadline())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The auction checkout deadline has passed");
        }
        validateCheckout(request);
        String shipping = request.shippingMethod().trim().toUpperCase(Locale.ROOT);
        double shippingFee = "STANDARD".equals(shipping) ? STANDARD_DELIVERY_FEE : 0d;
        Order order = new Order();
        order.setUserId(winner.getUaId());
        order.setTotalPrice(roundMoney(auction.getCurrentPrice() + shippingFee));
        order.setSubtotal(auction.getCurrentPrice());
        order.setDiscountAmount(0d);
        order.setStoreId(auction.getStore() == null ? null : auction.getStore().getStoreId());
        order.setStoreName(auction.getStore() == null ? "Optracard Official Store" : auction.getStore().getStoreName());
        order.setSource(auction.getSource());
        order.setAuctionId(auction.getAuctionId());
        order.setPaymentMethod(request.paymentMethod().trim());
        order.setPaymentStatus("PAID");
        order.setStatus("Processing");
        order.setShippingMethod(shipping);
        order.setShippingFee(shippingFee);
        order.setRecipientName(request.recipientName().trim());
        order.setRecipientPhone(request.recipientPhone().trim());
        order.setShippingAddress("PICKUP".equals(shipping) ? null : request.shippingAddress().trim());
        orderRepository.save(order);
        auction.setStatus(AuctionStatus.COMPLETED.name());
        auction.setUpdatedAt(now());
        auctionRepository.saveAndFlush(auction);
        return toResponse(auction);
    }

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void advanceAuctionLifecycle() {
        LocalDateTime current = now();
        List<Auction> changed = auctionRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(auction -> advanceOne(auction, current))
                .toList();
        if (!changed.isEmpty()) auctionRepository.saveAll(changed);
    }

    private boolean advanceOne(Auction auction, LocalDateTime current) {
        AuctionStatus status = statusOf(auction);
        if (status == AuctionStatus.SCHEDULED && !current.isBefore(auction.getStartsAt())) {
            auction.setStatus(AuctionStatus.LIVE.name());
            auction.setUpdatedAt(current);
            return true;
        }
        if (status == AuctionStatus.LIVE && !current.isBefore(auction.getEndsAt())) {
            if (auction.getBidCount() == 0) {
                auction.setStatus(AuctionStatus.ENDED_NO_BIDS.name());
            } else {
                AuctionBid winningBid = bidRepository.findFirstByAuction_AuctionIdOrderByAmountDescCreatedAtAsc(auction.getAuctionId())
                        .orElseThrow(() -> new IllegalStateException("Auction bid count does not match bid history"));
                auction.setWinningUserId(winningBid.getBidderUserId());
                auction.setCheckoutDeadline(current.plus(CHECKOUT_WINDOW));
                auction.setStatus(AuctionStatus.AWAITING_CHECKOUT.name());
            }
            auction.setUpdatedAt(current);
            return true;
        }
        if (status == AuctionStatus.AWAITING_CHECKOUT && auction.getCheckoutDeadline() != null
                && !current.isBefore(auction.getCheckoutDeadline())) {
            auction.setStatus(AuctionStatus.CHECKOUT_EXPIRED.name());
            auction.setUpdatedAt(current);
            return true;
        }
        return false;
    }

    private void apply(Auction auction, AuctionRequest request) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Auction data is required");
        auction.setTitle(require(request.title(), "Auction title is required"));
        if (request.gameId() == null || !cardGameRepository.existsById(request.gameId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a supported card game");
        }
        if (request.startingPrice() == null || request.startingPrice() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Starting price must be greater than zero");
        }
        validateTiming(request.startsAt(), request.endsAt());
        auction.setGameId(request.gameId());
        auction.setType(blankToNull(request.type()));
        auction.setImageUrl(blankToNull(request.imageUrl()));
        auction.setDescription(blankToNull(request.description()));
        auction.setStartingPrice(roundMoney(request.startingPrice()));
        auction.setCurrentPrice(roundMoney(request.startingPrice()));
        auction.setBidCount(0);
        auction.setStartsAt(request.startsAt());
        auction.setEndsAt(request.endsAt());
        auction.setUpdatedAt(now());
    }

    private void validateTiming(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Auction end time must be after the start time");
        }
        Duration duration = Duration.between(startsAt, endsAt);
        if (duration.compareTo(MIN_DURATION) < 0 || duration.compareTo(MAX_DURATION) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Auction duration must be between 1 hour and 7 days");
        }
        if (endsAt.isBefore(now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Auction end time must be in the future");
        }
    }

    private void validateCheckout(AuctionCheckoutRequest request) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checkout data is required");
        require(request.recipientName(), "Recipient name is required");
        require(request.recipientPhone(), "Recipient phone is required");
        require(request.paymentMethod(), "Payment method is required");
        String shipping = request.shippingMethod() == null ? "" : request.shippingMethod().trim().toUpperCase(Locale.ROOT);
        if (!List.of("PICKUP", "STANDARD").contains(shipping)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose PICKUP or STANDARD delivery");
        }
        if ("STANDARD".equals(shipping)) require(request.shippingAddress(), "Shipping address is required for delivery");
    }

    private MarketplaceStore requireApprovedStore(Integer userId) {
        return storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(userId)
                .filter(store -> "APPROVED".equalsIgnoreCase(store.getStoreStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "An approved marketplace store is required to create an auction"));
    }

    private Auction requireOwnedAuction(User user, Integer auctionId) {
        Auction auction = findAuction(auctionId);
        if (!Objects.equals(user.getUaId(), auction.getSellerUserId()) || !"MARKETPLACE".equalsIgnoreCase(auction.getSource())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found");
        }
        return auction;
    }

    private Auction findAuction(Integer auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private void requireBidder(User user) {
        String role = roleOf(user);
        if (!List.of("USER", "SELLER").contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only buyers and sellers can place bids");
        }
    }

    private void requireRole(User user, String expected) {
        if (!expected.equals(roleOf(user))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller access required");
    }

    private void requireAdmin(User user) {
        if (!List.of("ADMIN", "SUPER_ADMIN").contains(roleOf(user))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required");
        }
    }

    private String roleOf(User user) {
        return user.getRole() == null ? "" : user.getRole().replace("ROLE_", "").trim().toUpperCase(Locale.ROOT);
    }

    private boolean isPubliclyVisible(Auction auction) {
        return List.of(AuctionStatus.SCHEDULED, AuctionStatus.LIVE, AuctionStatus.AWAITING_CHECKOUT,
                AuctionStatus.COMPLETED, AuctionStatus.ENDED_NO_BIDS, AuctionStatus.CHECKOUT_EXPIRED)
                .contains(statusOf(auction));
    }

    private boolean matches(String actual, String requested) {
        return requested == null || requested.isBlank() || "ALL".equalsIgnoreCase(requested)
                || (actual != null && actual.equalsIgnoreCase(requested));
    }

    private AuctionStatus statusOf(Auction auction) {
        return AuctionStatus.valueOf(auction.getStatus());
    }

    private AuctionResponse toResponse(Auction auction) {
        MarketplaceStore store = auction.getStore();
        Integer leadingUserId = bidRepository.findFirstByAuction_AuctionIdOrderByAmountDescCreatedAtAsc(auction.getAuctionId())
                .map(AuctionBid::getBidderUserId)
                .orElse(null);
        return new AuctionResponse(auction.getAuctionId(), auction.getTitle(),
                auction.getCardGame() == null ? "Uncategorized" : auction.getCardGame().getGameName(), auction.getType(),
                auction.getImageUrl(), auction.getDescription(), auction.getStartingPrice(), auction.getCurrentPrice(),
                auction.getBidCount(), auction.getStartsAt(), auction.getEndsAt(), auction.getCheckoutDeadline(), auction.getStatus(),
                auction.getSource(), store == null ? null : store.getStoreId(),
                store == null ? "Optracard Official Store" : store.getStoreName(), auction.getSellerUserId(),
                auction.getWinningUserId(), leadingUserId, auction.getReviewNote(), auction.getCreatedAt());
    }

    private AuctionBidResponse toBidResponse(AuctionBid bid) {
        return new AuctionBidResponse(bid.getId(), bid.getAmount(), bid.getBidderPublicCode(), bid.getCreatedAt());
    }

    private LocalDateTime now() { return LocalDateTime.now(BANGKOK); }
    private double roundMoney(double amount) { return Math.round(amount * 100d) / 100d; }
    private String blankToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private String require(String value, String message) {
        String normalized = blankToNull(value);
        if (normalized == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        return normalized;
    }
    private String publicBidderCode(Integer auctionId, Integer userId) {
        return "Bidder #" + Integer.toUnsignedString(Objects.hash(auctionId, userId, "optracard-auction"), 36).toUpperCase(Locale.ROOT);
    }
}
