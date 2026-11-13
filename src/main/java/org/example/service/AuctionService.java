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

import java.time.Clock;
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

    @org.springframework.beans.factory.annotation.Autowired
    private AuctionOperationsService operations;
    @org.springframework.beans.factory.annotation.Autowired
    private AuctionInventoryService inventory;
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;
    private final Clock clock;
    private final AuctionRepository auctionRepository;
    private final AuctionBidRepository bidRepository;
    private final MarketplaceStoreRepository storeRepository;
    private final CardGameRepository cardGameRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public AuctionService(AuctionRepository auctionRepository,
                          AuctionBidRepository bidRepository,
                          MarketplaceStoreRepository storeRepository,
                          CardGameRepository cardGameRepository,
                          UserRepository userRepository,
                          OrderRepository orderRepository) {
        this(auctionRepository, bidRepository, storeRepository, cardGameRepository, userRepository, orderRepository, Clock.system(BANGKOK));
    }

    AuctionService(AuctionRepository auctionRepository, AuctionBidRepository bidRepository,
                   MarketplaceStoreRepository storeRepository, CardGameRepository cardGameRepository,
                   UserRepository userRepository, OrderRepository orderRepository, Clock clock) {
        this.clock = clock;
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
    public List<AuctionResponse> myAuctions(String email) {
        User user=currentUser(email);requireBidder(user);
        return auctionRepository.findAllByOrderByCreatedAtDesc().stream()
            .filter(a->bidRepository.findFirstByAuction_AuctionIdAndBidderUserId(a.getAuctionId(),user.getUaId()).isPresent())
            .map(this::toResponse).toList();
    }

    @Transactional
    public AuctionResponse moderate(String email,Integer id,String action,String reason) {
        User actor=currentUser(email);
        if(!"SUPER_ADMIN".equals(roleOf(actor)))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        String note=require(reason,"A moderation reason is required");
        Auction a=auctionRepository.findByIdForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(List.of("AWAITING_CHECKOUT","COMPLETED").contains(a.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Winner orders require dispute review before intervention");
        if(!List.of("CLOSE","SUSPEND").contains(action))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose CLOSE or SUSPEND");
        String before=snapshot(a);a.setStatus("CLOSE".equals(action)?"CLOSED":"SUSPENDED");a.setReviewNote(note);a.setUpdatedAt(now());
        return saved(a,actor.getUaId(),"ADMIN_CANCEL",before);
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
        return saved(auction,seller.getUaId(),"CREATE","");
    }

    @Transactional
    public AuctionResponse updateSellerAuction(String email, Integer auctionId, AuctionRequest request) {
        User seller = currentUser(email);
        requireRole(seller, "SELLER");
        Auction auction = requireOwnedAuction(seller, auctionId);
        String before = snapshot(auction);
        AuctionStatus status = statusOf(auction);
        if (!List.of(AuctionStatus.DRAFT, AuctionStatus.PENDING_REVIEW, AuctionStatus.REJECTED).contains(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only a draft, pending review or rejected auction can be edited");
        }
        apply(auction, request);
        // Any edit after the seller submitted the listing needs another review.
        if (status != AuctionStatus.DRAFT) {
            auction.setStatus(AuctionStatus.PENDING_REVIEW.name());
            auction.setReviewNote(null);
        }
        return saved(auction,seller.getUaId(),"EDIT",before);
    }

    @Transactional
    public AuctionResponse submitSellerAuction(String email, Integer auctionId) {
        User seller = currentUser(email);
        requireRole(seller, "SELLER");
        Auction auction = requireOwnedAuction(seller, auctionId);
        String before = snapshot(auction);
        if (!List.of(AuctionStatus.DRAFT, AuctionStatus.REJECTED).contains(statusOf(auction))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a draft or rejected auction can be submitted");
        }
        validateTiming(auction.getStartsAt(), auction.getEndsAt());
        if(inventory!=null) inventory.reserve(auction);
        auction.setStatus(AuctionStatus.PENDING_REVIEW.name());
        auction.setReviewNote(null);
        return saved(auction,seller.getUaId(),"SUBMIT",before);
    }

    @Transactional
    public AuctionResponse cancelSellerAuction(String email, Integer auctionId) {
        User seller = currentUser(email);
        requireRole(seller, "SELLER");
        Auction auction = requireOwnedAuction(seller, auctionId);
        String before = snapshot(auction);
        AuctionStatus status = statusOf(auction);
        if (!(List.of(AuctionStatus.DRAFT,AuctionStatus.PENDING_REVIEW,AuctionStatus.REJECTED,AuctionStatus.SCHEDULED).contains(status)
                || (status == AuctionStatus.LIVE && auction.getBidCount() == 0))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This auction can no longer be cancelled");
        }
        auction.setStatus(AuctionStatus.CANCELLED.name());
        auction.setUpdatedAt(now());
        return saved(auction,seller.getUaId(),"SELLER_CANCEL",before);
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
        return saved(replacement,seller.getUaId(),"RELIST","");
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
        if(inventory!=null) inventory.reserve(auction);
        auction.setStatus(auction.getStartsAt().isAfter(now()) ? AuctionStatus.SCHEDULED.name() : AuctionStatus.LIVE.name());
        return saved(auction,admin.getUaId(),"CREATE_OFFICIAL","");
    }

    @Transactional
    public AuctionResponse reviewAuction(String email, Integer auctionId, boolean approved, String note) {
        User admin = currentUser(email);
        requireAdmin(admin);
        Auction auction = auctionRepository.findByIdForUpdate(auctionId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Auction not found"));
        String before = snapshot(auction);
        if (!"MARKETPLACE".equalsIgnoreCase(auction.getSource()) || statusOf(auction) != AuctionStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This marketplace auction is not waiting for review");
        }
        if (!approved) {
            auction.setStatus(AuctionStatus.REJECTED.name());
            auction.setReviewNote(require(note,"A rejection reason is required"));
        } else {
            validateTiming(auction.getStartsAt(), auction.getEndsAt());
            if(inventory!=null) inventory.reserve(auction);
            auction.setStatus(auction.getStartsAt().isAfter(now()) ? AuctionStatus.SCHEDULED.name() : AuctionStatus.LIVE.name());
            auction.setReviewNote(null);
        }
        auction.setUpdatedAt(now());
        return saved(auction,admin.getUaId(),(approved ? "APPROVE" : "REJECT"),before);
    }

    @Transactional
    public AuctionResponse cancelByAdmin(String email, Integer auctionId, String note) {
        User admin = currentUser(email);
        requireAdmin(admin);
        Auction auction = auctionRepository.findByIdForUpdate(auctionId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Auction not found"));
        String before = snapshot(auction);
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
        return saved(auction,admin.getUaId(),"ADMIN_CANCEL",before);
    }

    @Transactional
    public AuctionBidResponse placeBid(String email, Integer auctionId, AuctionBidRequest request) {
        User bidder = currentUser(email);
        requireBidder(bidder);
        if(operations!=null)operations.requireAllowedBidder(bidder.getUaId());
        if (request == null || request.amount() == null || !Double.isFinite(request.amount()) || request.amount() <= 0 || request.amount()>1000000000d || java.math.BigDecimal.valueOf(request.amount()).stripTrailingZeros().scale()>2) {
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
        double required = auction.getBidCount() == 0 ? auction.getStartingPrice() : auction.getCurrentPrice() + increment(auction);
        if (request.amount() < required) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.US, "Your bid must be at least %.2f", required));
        }
        Integer previousLeader = bidRepository.findFirstByAuction_AuctionIdOrderByAmountDescCreatedAtAsc(auctionId).map(AuctionBid::getBidderUserId).orElse(null);
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
        if (antiSniping(auction) > 0 && !bidTime.isBefore(auction.getEndsAt().minusMinutes(antiSniping(auction)))) {
            auction.setEndsAt(auction.getEndsAt().plusMinutes(antiSniping(auction)));
        }
        auction.setUpdatedAt(bidTime);
        auctionRepository.saveAndFlush(auction);
        if(operations!=null && previousLeader!=null && !previousLeader.equals(bidder.getUaId()))
            operations.notify(previousLeader,auctionId,"OUTBID","You have been outbid on "+auction.getTitle(),"outbid:"+auctionId+":"+auction.getBidCount()+":"+previousLeader);
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
        return saved(auction,winner.getUaId(),"CHECKOUT","");
    }

    @Scheduled(fixedDelay = 30_000)
    public void advanceAuctionLifecycle() {
        LocalDateTime current = now();
        // Requests and the lifecycle job must lock the same current database rows.
        auctionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(Auction::getAuctionId).sorted().forEach(id -> {
                    Runnable advance = () -> auctionRepository.findByIdForUpdate(id).ifPresent(auction -> {
                        if (advanceOne(auction, current)) auctionRepository.saveAndFlush(auction);
                        if(operations!=null && "AWAITING_CHECKOUT".equals(auction.getStatus()) && auction.getCheckoutDeadline()!=null
                            && current.isBefore(auction.getCheckoutDeadline()) && !current.isBefore(auction.getCheckoutDeadline().minusHours(1)))
                            operations.notify(auction.getWinningUserId(),id,"CHECKOUT_REMINDER","Checkout deadline approaches for "+auction.getTitle(),"reminder:"+id);
                    });
                    if(transactionManager==null)advance.run();
                    else new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(tx->advance.run());
                });
    }

    private boolean advanceOne(Auction auction, LocalDateTime current) {
        boolean changed = false;
        if (statusOf(auction) == AuctionStatus.SCHEDULED && !current.isBefore(auction.getStartsAt())) {
            auction.setStatus(AuctionStatus.LIVE.name());
            changed = true;
        }
        if (statusOf(auction) == AuctionStatus.LIVE && !current.isBefore(auction.getEndsAt())) {
            if (auction.getBidCount() == 0) {
                auction.setStatus(AuctionStatus.ENDED_NO_BIDS.name());
            } else {
                AuctionBid winningBid = bidRepository.findFirstByAuction_AuctionIdOrderByAmountDescCreatedAtAsc(auction.getAuctionId())
                        .orElseThrow(() -> new IllegalStateException("Auction bid count does not match bid history"));
                auction.setWinningUserId(winningBid.getBidderUserId());
                auction.setCheckoutDeadline(auction.getEndsAt().plusHours(checkoutHours(auction)));
                auction.setStatus(AuctionStatus.AWAITING_CHECKOUT.name());
            }
            changed = true;
        }
        if (statusOf(auction) == AuctionStatus.AWAITING_CHECKOUT && auction.getCheckoutDeadline() != null
                && !current.isBefore(auction.getCheckoutDeadline())) {
            auction.setStatus(AuctionStatus.CHECKOUT_EXPIRED.name());
            changed = true;
        }
        if (changed) {
            auction.setUpdatedAt(current);
            if(inventory!=null && List.of("ENDED_NO_BIDS","CHECKOUT_EXPIRED").contains(auction.getStatus()))inventory.release(auction,false);
            if(operations!=null) {
                operations.audit(null,auction.getAuctionId(),"LIFECYCLE","",snapshot(auction));
                if("AWAITING_CHECKOUT".equals(auction.getStatus())) {
                    operations.notify(auction.getWinningUserId(),auction.getAuctionId(),"WON","You won "+auction.getTitle()+". Checkout by "+auction.getCheckoutDeadline(),"won:"+auction.getAuctionId());
                    operations.notify(auction.getSellerUserId(),auction.getAuctionId(),"WINNER_SELECTED","A winner was selected for "+auction.getTitle(),"winner:"+auction.getAuctionId());
                }
            }
        }
        return changed;
    }

    private void apply(Auction auction, AuctionRequest request) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Auction data is required");
        if (request.bidIncrement() != null) {
            auction.setBidIncrement(validateBidIncrement(request.bidIncrement()));
        } else if (auction.getBidIncrement() == null) {
            var settings = operations == null ? new org.example.model.AuctionSettings() : operations.settings();
            auction.setBidIncrement(settings.getBidIncrement()); auction.setCheckoutHours(settings.getCheckoutHours());
            auction.setAntiSnipingMinutes(settings.getAntiSnipingMinutes());
        }
        if(inventory!=null) inventory.bind(auction,request);
        auction.setTitle(require(request.title(), "Auction title is required"));
        if (request.gameId() == null || !cardGameRepository.existsById(request.gameId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a supported card game");
        }
        if (request.startingPrice() == null || (!Double.isFinite(request.startingPrice()) || request.startingPrice() <= 0 || request.startingPrice()>1000000000d || java.math.BigDecimal.valueOf(request.startingPrice()).stripTrailingZeros().scale()>2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Starting price must be greater than zero");
        }
        validateTiming(request.startsAt(), request.endsAt());
        auction.setGameId(request.gameId());
        auction.setType(blankToNull(request.type()));
        auction.setImageUrl(blankToNull(request.imageUrl()));
        auction.setDescription(blankToNull(request.description()));
        auction.setProductSet(blankToNull(request.productSet()));
        auction.setLanguage(blankToNull(request.language()));
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
        if (duration.compareTo(Duration.ofHours(operations==null?1:operations.settings().getMinDurationHours())) < 0 || duration.compareTo(Duration.ofHours(operations==null?168:operations.settings().getMaxDurationHours())) > 0) {
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
        Auction auction = auctionRepository.findByIdForUpdate(auctionId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Auction not found"));
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
                auction.getWinningUserId(), leadingUserId, auction.getReviewNote(), auction.getCreatedAt(), increment(auction),checkoutHours(auction),antiSniping(auction),auction.getProductId(),auction.getCardCondition(),auction.getGrading(),auction.getEvidenceImages()==null||auction.getEvidenceImages().isBlank()?List.of():List.of(auction.getEvidenceImages().split("\\n")),auction.getProductSet(),auction.getLanguage());
    }

    private AuctionBidResponse toBidResponse(AuctionBid bid) {
        return new AuctionBidResponse(bid.getId(), bid.getAmount(), bid.getBidderPublicCode(), bid.getCreatedAt());
    }

    private double increment(Auction a) {return a.getBidIncrement()==null?50d:a.getBidIncrement();}
    private double validateBidIncrement(Double value) {
        if (!Double.isFinite(value) || value <= 0 || value > 1_000_000d
                || java.math.BigDecimal.valueOf(value).stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Minimum bid increment must be greater than zero and use at most two decimal places");
        }
        return roundMoney(value);
    }
    private int checkoutHours(Auction a) {return a.getCheckoutHours()==null?24:a.getCheckoutHours();}
    private int antiSniping(Auction a) {return a.getAntiSnipingMinutes()==null?5:a.getAntiSnipingMinutes();}
    private String snapshot(Auction a) {return operations==null?"":operations.snapshot(a);}
    private AuctionResponse saved(Auction a,Integer actor,String action,String before) {
        a.setUpdatedAt(now());
        auctionRepository.saveAndFlush(a);
        if(inventory!=null && List.of("CANCELLED","CLOSED","REJECTED","ENDED_NO_BIDS","CHECKOUT_EXPIRED","COMPLETED").contains(a.getStatus())) inventory.release(a,"COMPLETED".equals(a.getStatus()));
        if(operations!=null) {
            operations.audit(actor,a.getAuctionId(),action,before,snapshot(a));
            if("SUBMIT".equals(action)) operations.notifyAdmins(a);
            if(List.of("APPROVE","REJECT","ADMIN_CANCEL","SELLER_CANCEL","CHECKOUT").contains(action))
                operations.notify(a.getSellerUserId(),a.getAuctionId(),action,"Auction "+a.getTitle()+": "+a.getStatus()+ (a.getReviewNote()==null?"":". "+a.getReviewNote()),action+":"+a.getAuctionId()+":"+a.getUpdatedAt());
        }
        return toResponse(a);
    }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
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
