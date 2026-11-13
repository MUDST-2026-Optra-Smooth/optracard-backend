package org.example.service;

import org.example.dto.AuctionBidRequest;
import org.example.dto.AuctionCheckoutRequest;
import org.example.dto.AuctionRequest;
import org.example.model.Auction;
import org.example.model.AuctionBid;
import org.example.model.AuctionStatus;
import org.example.model.MarketplaceStore;
import org.example.model.Order;
import org.example.model.User;
import org.example.repository.AuctionBidRepository;
import org.example.repository.AuctionRepository;
import org.example.repository.CardGameRepository;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.OrderRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {
    @Mock private AuctionRepository auctionRepository;
    @Mock private AuctionBidRepository bidRepository;
    @Mock private MarketplaceStoreRepository storeRepository;
    @Mock private CardGameRepository cardGameRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;

    private AuctionService service;

    @BeforeEach
    void setUp() {
        service = new AuctionService(auctionRepository, bidRepository, storeRepository, cardGameRepository, userRepository, orderRepository);
        lenient().when(auctionRepository.saveAndFlush(any(Auction.class))).thenAnswer(invocation -> {
            Auction auction = invocation.getArgument(0);
            if (auction.getAuctionId() == null) auction.setAuctionId(91);
            return auction;
        });
    }

    @Test
    void sellerWithApprovedStoreCreatesDraftWithinAllowedWindow() {
        User seller = user(7, "SELLER");
        MarketplaceStore store = store(22, 7, "APPROVED");
        when(userRepository.findByEmail("seller@example.test")).thenReturn(Optional.of(seller));
        when(storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(7)).thenReturn(Optional.of(store));
        when(cardGameRepository.existsById(1)).thenReturn(true);

        var result = service.createSellerAuction("seller@example.test", request(2));

        ArgumentCaptor<Auction> captor = ArgumentCaptor.forClass(Auction.class);
        verify(auctionRepository).saveAndFlush(captor.capture());
        assertEquals("MARKETPLACE", captor.getValue().getSource());
        assertEquals(AuctionStatus.DRAFT.name(), captor.getValue().getStatus());
        assertEquals(7, captor.getValue().getSellerUserId());
        assertEquals("DRAFT", result.status());
    }

    @Test
    void sellerCreationRejectsUnapprovedStoreAndOutOfRangeDuration() {
        User seller = user(7, "SELLER");
        when(userRepository.findByEmail("seller@example.test")).thenReturn(Optional.of(seller));
        when(storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(7)).thenReturn(Optional.of(store(22, 7, "PENDING")));

        ResponseStatusException storeError = assertThrows(ResponseStatusException.class,
                () -> service.createSellerAuction("seller@example.test", request(2)));
        assertEquals(HttpStatus.FORBIDDEN, storeError.getStatusCode());

        when(storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(7)).thenReturn(Optional.of(store(22, 7, "APPROVED")));
        when(cardGameRepository.existsById(1)).thenReturn(true);
        ResponseStatusException timingError = assertThrows(ResponseStatusException.class,
                () -> service.createSellerAuction("seller@example.test", request(8)));
        assertEquals(HttpStatus.BAD_REQUEST, timingError.getStatusCode());
    }

    @Test
    void bidRejectsOwnAndTooSmallOffersThenPersistsTheMinimumValidIncrease() {
        Auction auction = auction(41, AuctionStatus.LIVE, 7, 1_000d, 1_200d, 2);
        User seller = user(7, "SELLER");
        User buyer = user(8, "USER");
        when(userRepository.findByEmail("seller@example.test")).thenReturn(Optional.of(seller));
        when(userRepository.findByEmail("buyer@example.test")).thenReturn(Optional.of(buyer));
        when(auctionRepository.findByIdForUpdate(41)).thenReturn(Optional.of(auction));

        ResponseStatusException ownBid = assertThrows(ResponseStatusException.class,
                () -> service.placeBid("seller@example.test", 41, new AuctionBidRequest(1_250d)));
        assertEquals(HttpStatus.FORBIDDEN, ownBid.getStatusCode());

        ResponseStatusException tooSmall = assertThrows(ResponseStatusException.class,
                () -> service.placeBid("buyer@example.test", 41, new AuctionBidRequest(1_249d)));
        assertEquals(HttpStatus.BAD_REQUEST, tooSmall.getStatusCode());

        var bid = service.placeBid("buyer@example.test", 41, new AuctionBidRequest(1_250d));
        assertEquals(1_250d, bid.amount());
        assertEquals(1_250d, auction.getCurrentPrice());
        assertEquals(3, auction.getBidCount());
        verify(bidRepository).save(any(AuctionBid.class));
    }

    @Test
    void bidUsesTheIncrementConfiguredOnItsAuction() {
        Auction auction = auction(44, AuctionStatus.LIVE, 7, 1_000d, 1_200d, 1);
        auction.setBidIncrement(125d);
        User buyer = user(8, "USER");
        when(userRepository.findByEmail("buyer@example.test")).thenReturn(Optional.of(buyer));
        when(auctionRepository.findByIdForUpdate(44)).thenReturn(Optional.of(auction));

        ResponseStatusException tooSmall = assertThrows(ResponseStatusException.class,
                () -> service.placeBid("buyer@example.test", 44, new AuctionBidRequest(1_324d)));
        assertEquals(HttpStatus.BAD_REQUEST, tooSmall.getStatusCode());

        var bid = service.placeBid("buyer@example.test", 44, new AuctionBidRequest(1_325d));
        assertEquals(1_325d, bid.amount());
    }

    @Test
    void lifecycleSelectsHighestEarliestBidderAndExpiresCheckoutWindow() {
        Auction auction = auction(42, AuctionStatus.LIVE, 7, 500d, 750d, 2);
        auction.setEndsAt(now().minusMinutes(1));
        AuctionBid winner = new AuctionBid();
        winner.setBidderUserId(8);
        when(auctionRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(auction));
        when(auctionRepository.findByIdForUpdate(42)).thenReturn(Optional.of(auction));
        when(bidRepository.findFirstByAuction_AuctionIdOrderByAmountDescCreatedAtAsc(42)).thenReturn(Optional.of(winner));

        service.advanceAuctionLifecycle();

        assertEquals(AuctionStatus.AWAITING_CHECKOUT.name(), auction.getStatus());
        assertEquals(8, auction.getWinningUserId());
        verify(auctionRepository).saveAndFlush(auction);

        auction.setCheckoutDeadline(now().minusSeconds(1));
        service.advanceAuctionLifecycle();
        assertEquals(AuctionStatus.CHECKOUT_EXPIRED.name(), auction.getStatus());
    }

    @Test
    void winnerCheckoutCreatesMarketplaceOrderWithDeliveryFeeAndValidatesAddress() {
        Auction auction = auction(43, AuctionStatus.AWAITING_CHECKOUT, 7, 500d, 800d, 1);
        auction.setWinningUserId(8);
        auction.setCheckoutDeadline(now().plusHours(3));
        auction.setStore(store(22, 7, "APPROVED"));
        User winner = user(8, "USER");
        when(userRepository.findByEmail("buyer@example.test")).thenReturn(Optional.of(winner));
        when(auctionRepository.findByIdForUpdate(43)).thenReturn(Optional.of(auction));

        ResponseStatusException missingAddress = assertThrows(ResponseStatusException.class,
                () -> service.checkout("buyer@example.test", 43, new AuctionCheckoutRequest("Ash", "0800000000", "", "STANDARD", "QR")));
        assertEquals(HttpStatus.BAD_REQUEST, missingAddress.getStatusCode());

        service.checkout("buyer@example.test", 43, new AuctionCheckoutRequest("Ash", "0800000000", "Bangkok", "STANDARD", "QR"));
        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(order.capture());
        assertEquals(850d, order.getValue().getTotalPrice());
        assertEquals(50d, order.getValue().getShippingFee());
        assertEquals(AuctionStatus.COMPLETED.name(), auction.getStatus());
    }

    @Test
    void publicListExcludesDraftsAndAdminReviewMovesOnlyPendingMarketplaceAuctions() {
        Auction draft = auction(1, AuctionStatus.DRAFT, 7, 500d, 500d, 0);
        Auction live = auction(2, AuctionStatus.LIVE, 7, 500d, 500d, 0);
        when(auctionRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(draft, live));
        assertEquals(List.of(2), service.listPublic(null, "ALL", "ALL").stream().map(response -> response.id()).toList());

        User admin = user(99, "ADMIN");
        Auction pending = auction(3, AuctionStatus.PENDING_REVIEW, 7, 500d, 500d, 0);
        when(userRepository.findByEmail("admin@example.test")).thenReturn(Optional.of(admin));
        when(auctionRepository.findByIdForUpdate(3)).thenReturn(Optional.of(pending));
        var reviewed = service.reviewAuction("admin@example.test", 3, false, "Need proof of condition");
        assertEquals(AuctionStatus.REJECTED.name(), reviewed.status());
        assertEquals("Need proof of condition", reviewed.reviewNote());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void coversPublicFiltersSellerLifecycleAdminActionsAndValidationBranches() {
        User seller = user(7, "SELLER");
        User buyer = user(8, "USER");
        User admin = user(99, "ADMIN");
        when(userRepository.findByEmail("seller@example.test")).thenReturn(Optional.of(seller));
        when(userRepository.findByEmail("buyer@example.test")).thenReturn(Optional.of(buyer));
        when(userRepository.findByEmail("admin@example.test")).thenReturn(Optional.of(admin));

        Auction live = auction(51, AuctionStatus.LIVE, 7, 100, 100, 0);
        live.setSource("OFFICIAL");
        when(auctionRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(live));
        assertEquals(1, service.listPublic("", "official", "live").size());
        assertThrows(ResponseStatusException.class, () -> service.getPublic(999));
        when(auctionRepository.findById(51)).thenReturn(Optional.of(live));
        assertEquals(51, service.getPublic(51).id());
        when(bidRepository.findByAuction_AuctionIdOrderByCreatedAtDesc(51)).thenReturn(List.of());
        assertEquals(0, service.bidHistory(51).size());
        when(auctionRepository.findBySellerUserIdOrderByCreatedAtDesc(7)).thenReturn(List.of(live));
        assertEquals(1, service.sellerAuctions("seller@example.test").size());
        when(bidRepository.findFirstByAuction_AuctionIdAndBidderUserId(51, 8)).thenReturn(Optional.of(new AuctionBid()));
        assertEquals(1, service.myAuctions("buyer@example.test").size());

        Auction draft = auction(52, AuctionStatus.DRAFT, 7, 100, 100, 0);
        when(auctionRepository.findByIdForUpdate(52)).thenReturn(Optional.of(draft));
        when(cardGameRepository.existsById(1)).thenReturn(true);
        assertEquals("PENDING_REVIEW", service.submitSellerAuction("seller@example.test", 52).status());

        Auction pending = auction(53, AuctionStatus.PENDING_REVIEW, 7, 100, 100, 0);
        when(auctionRepository.findByIdForUpdate(53)).thenReturn(Optional.of(pending));
        assertEquals("PENDING_REVIEW", service.updateSellerAuction("seller@example.test", 53, request(2)).status());
        Auction scheduled = auction(54, AuctionStatus.SCHEDULED, 7, 100, 100, 0);
        when(auctionRepository.findByIdForUpdate(54)).thenReturn(Optional.of(scheduled));
        assertEquals("CANCELLED", service.cancelSellerAuction("seller@example.test", 54).status());
        Auction ended = auction(55, AuctionStatus.ENDED_NO_BIDS, 7, 100, 100, 0);
        when(auctionRepository.findByIdForUpdate(55)).thenReturn(Optional.of(ended));
        assertEquals("DRAFT", service.relistSellerAuction("seller@example.test", 55, request(2)).status());

        Auction adminDraft = auction(58, AuctionStatus.DRAFT, 7, 100, 100, 0);
        when(auctionRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(adminDraft, pending));
        assertEquals(1, service.adminAuctions("DRAFT").size());
        when(cardGameRepository.existsById(1)).thenReturn(true);
        AuctionRequest future = new AuctionRequest("Official", 1, "Single", "", "detail", 100d, 50d, now().plusHours(2), now().plusHours(4), null, null, null, List.of(), null, null);
        assertEquals("SCHEDULED", service.createOfficialAuction("admin@example.test", future).status());
        Auction review = auction(56, AuctionStatus.PENDING_REVIEW, 7, 100, 100, 0);
        when(auctionRepository.findByIdForUpdate(56)).thenReturn(Optional.of(review));
        assertEquals("LIVE", service.reviewAuction("admin@example.test", 56, true, "").status());
        Auction liveWithBid = auction(57, AuctionStatus.LIVE, 7, 100, 150, 1);
        when(auctionRepository.findByIdForUpdate(57)).thenReturn(Optional.of(liveWithBid));
        assertEquals("CANCELLED", service.cancelByAdmin("admin@example.test", 57, "policy").status());

        assertThrows(ResponseStatusException.class, () -> service.placeBid("buyer@example.test", 51, null));
        assertThrows(ResponseStatusException.class, () -> service.placeBid("admin@example.test", 51, new AuctionBidRequest(200d)));
    }

    private AuctionRequest request(int days) {
        LocalDateTime start = now().plusMinutes(5);
        return new AuctionRequest("E2E Test Card", 1, "Single", "", "Mint", 500d, start, start.plusDays(days));
    }

    private Auction auction(int id, AuctionStatus status, int sellerUserId, double starting, double current, int bidCount) {
        Auction auction = new Auction();
        auction.setAuctionId(id);
        auction.setSellerUserId(sellerUserId);
        auction.setSource("MARKETPLACE");
        auction.setStatus(status.name());
        auction.setTitle("Auction " + id);
        auction.setStartingPrice(starting);
        auction.setCurrentPrice(current);
        auction.setBidCount(bidCount);
        auction.setStartsAt(now().minusHours(1));
        auction.setEndsAt(now().plusHours(2));
        auction.setCreatedAt(now());
        return auction;
    }

    private User user(int id, String role) {
        User user = new User();
        user.setUaId(id);
        user.setRole(role);
        return user;
    }

    private MarketplaceStore store(int id, int sellerUserId, String status) {
        MarketplaceStore store = new MarketplaceStore();
        store.setStoreId(id);
        store.setSellerUserId(sellerUserId);
        store.setStoreName("Card Corner");
        store.setStoreStatus(status);
        return store;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Bangkok"));
    }
}
