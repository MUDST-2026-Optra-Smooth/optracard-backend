package org.example.service;

import org.example.dto.*;
import org.example.model.*;
import org.example.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AuctionServiceTest {
    AuctionRepository auctions; AuctionBidRepository bids; UserRepository users; OrderRepository orders;
    MarketplaceStoreRepository stores; CardGameRepository games; AuctionService service;
    final LocalDateTime now = LocalDateTime.of(2026,11,11,12,0);
    Auction auction; User buyer;
    @BeforeEach void setup() {
        auctions=mock(AuctionRepository.class); bids=mock(AuctionBidRepository.class);
        users=mock(UserRepository.class); orders=mock(OrderRepository.class);
        stores=mock(MarketplaceStoreRepository.class); games=mock(CardGameRepository.class);
        service=new AuctionService(auctions,bids,stores,games,users,orders,
            Clock.fixed(now.atZone(ZoneId.of("Asia/Bangkok")).toInstant(),ZoneId.of("Asia/Bangkok")));
        buyer=user(2,"USER"); when(users.findByEmail("buyer")).thenReturn(Optional.of(buyer));
        auction=new Auction(); auction.setAuctionId(1); auction.setSellerUserId(9);
        auction.setTitle("Test card"); auction.setSource("MARKETPLACE"); auction.setStatus("LIVE");
        auction.setStartingPrice(100d); auction.setCurrentPrice(100d); auction.setBidCount(0);
        auction.setStartsAt(now.minusHours(1)); auction.setEndsAt(now.plusHours(1));
        when(auctions.findByIdForUpdate(1)).thenReturn(Optional.of(auction));
        when(auctions.findById(1)).thenReturn(Optional.of(auction));
        when(auctions.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
    }
    User user(int id,String role) { User u=new User();u.setUaId(id);u.setRole(role);return u; }
    void fails(int status, Runnable action) {
        assertEquals(status,assertThrows(ResponseStatusException.class,action::run).getStatusCode().value());
    }
    @Test void rejectsBelowOpeningPrice() { fails(400,()->service.placeBid("buyer",1,new AuctionBidRequest(99d))); verify(bids,never()).save(any()); }
    @Test void rejectsBelowIncrement() { auction.setBidCount(1);fails(400,()->service.placeBid("buyer",1,new AuctionBidRequest(149d))); }
    @Test void acceptsExactMinimum() { service.placeBid("buyer",1,new AuctionBidRequest(100d));assertEquals(1,auction.getBidCount());assertEquals(100d,auction.getCurrentPrice()); }
    @Test void rejectsOwnerBid() { auction.setSellerUserId(2);fails(403,()->service.placeBid("buyer",1,new AuctionBidRequest(100d))); }
    @Test void rejectsBidAtEnd() { auction.setEndsAt(now);fails(409,()->service.placeBid("buyer",1,new AuctionBidRequest(100d))); }
    @ParameterizedTest @ValueSource(doubles={Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,0,-1})
    void rejectsNonFiniteOrNonPositiveBid(double amount) { fails(400,()->service.placeBid("buyer",1,new AuctionBidRequest(amount))); }
    @ParameterizedTest @ValueSource(strings={"ADMIN","SUPER_ADMIN","UNKNOWN"})
    void rejectsStaffAndUnknownBidders(String role) { buyer.setRole(role);fails(403,()->service.placeBid("buyer",1,new AuctionBidRequest(100d))); }
    @Test void extendsByFiveMinutesAtWindowBoundary() { auction.setEndsAt(now.plusMinutes(5));service.placeBid("buyer",1,new AuctionBidRequest(100d));assertEquals(now.plusMinutes(10),auction.getEndsAt()); }
    @Test void doesNotExtendEarlierBid() { service.placeBid("buyer",1,new AuctionBidRequest(100d));assertEquals(now.plusHours(1),auction.getEndsAt()); }
    @Test void secondBidUsesUpdatedMinimum() { service.placeBid("buyer",1,new AuctionBidRequest(100d));fails(400,()->service.placeBid("buyer",1,new AuctionBidRequest(100d)));assertEquals(1,auction.getBidCount()); }
    void lifecycle() { when(auctions.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(auction));service.advanceAuctionLifecycle();verify(auctions).findByIdForUpdate(1); }
    @Test void scheduledBecomesLive() { auction.setStatus("SCHEDULED");lifecycle();assertEquals("LIVE",auction.getStatus()); }
    @Test void overdueScheduledEndsInSameRun() { auction.setStatus("SCHEDULED");auction.setEndsAt(now.minusMinutes(1));lifecycle();assertEquals("ENDED_NO_BIDS",auction.getStatus()); }
    @Test void endsWithoutBids() { auction.setEndsAt(now);lifecycle();assertEquals("ENDED_NO_BIDS",auction.getStatus()); }
    @Test void delayedJobDoesNotExtendCheckoutWindow() {
        auction.setEndsAt(now.minusHours(25));auction.setBidCount(1);
        AuctionBid bid=new AuctionBid();bid.setBidderUserId(2);
        when(bids.findFirstByAuction_AuctionIdOrderByAmountDescCreatedAtAsc(1)).thenReturn(Optional.of(bid));
        lifecycle();assertEquals("CHECKOUT_EXPIRED",auction.getStatus());assertEquals(now.minusHours(1),auction.getCheckoutDeadline());
    }
    AuctionCheckoutRequest checkout() { return new AuctionCheckoutRequest("Buyer","0800000000",null,"PICKUP","PROMPTPAY"); }
    void awaiting() { auction.setStatus("AWAITING_CHECKOUT");auction.setWinningUserId(2);auction.setCheckoutDeadline(now.plusHours(1)); }
    @Test void onlyWinnerCanCheckout() { awaiting();auction.setWinningUserId(3);fails(403,()->service.checkout("buyer",1,checkout()));verifyNoInteractions(orders); }
    @Test void winnerCreatesExactlyOneOrder() {
        awaiting();service.checkout("buyer",1,checkout());fails(403,()->service.checkout("buyer",1,checkout()));
        verify(orders,times(1)).save(argThat(o->o.getAuctionId()==1&&o.getUserId()==2));assertEquals("COMPLETED",auction.getStatus());
    }
    @Test void rejectsCheckoutAtDeadline() { awaiting();auction.setCheckoutDeadline(now);fails(403,()->service.checkout("buyer",1,checkout()));verifyNoInteractions(orders); }
    @Test void sellerCannotEditLiveEvenWithoutBids() { buyer.setRole("SELLER");auction.setSellerUserId(2);fails(409,()->service.updateSellerAuction("buyer",1,null)); }
    @Test void sellerCanCancelDraft() { buyer.setRole("SELLER");auction.setSellerUserId(2);auction.setStatus("DRAFT");service.cancelSellerAuction("buyer",1);assertEquals("CANCELLED",auction.getStatus()); }
    @Test void sellerCannotCancelLiveWithBids() { buyer.setRole("SELLER");auction.setSellerUserId(2);auction.setBidCount(1);fails(409,()->service.cancelSellerAuction("buyer",1)); }
    @Test void buyerCannotModerate() { fails(403,()->service.reviewAuction("buyer",1,true,null)); }
    @Test void adminRejectsPendingAuction() { buyer.setRole("ADMIN");auction.setStatus("PENDING_REVIEW");service.reviewAuction("buyer",1,false,"Missing proof");assertEquals("REJECTED",auction.getStatus());assertEquals("Missing proof",auction.getReviewNote()); }
    @Test void superadminApprovesPendingAuction() { buyer.setRole("SUPER_ADMIN");auction.setStatus("PENDING_REVIEW");service.reviewAuction("buyer",1,true,null);assertEquals("LIVE",auction.getStatus()); }
    @Test void adminNeedsReasonToCancelBids() { buyer.setRole("ADMIN");auction.setBidCount(1);fails(400,()->service.cancelByAdmin("buyer",1," "));service.cancelByAdmin("buyer",1,"Counterfeit report");assertEquals("CANCELLED",auction.getStatus()); }
}
