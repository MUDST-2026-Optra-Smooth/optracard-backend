package org.example.controller;

import org.example.dto.AuctionResponse;
import org.example.model.AuctionBid;
import org.example.model.AuctionEvent;
import org.example.model.AuctionSettings;
import org.example.model.Order;
import org.example.model.AuctionBidderRestriction;
import org.example.repository.AuctionBidRepository;
import org.example.repository.OrderRepository;
import org.example.service.AuctionDisputeService;
import org.example.service.AuctionOperationsService;
import org.example.service.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuperAdminAuctionControllerTest {
    @Mock AuctionService auctions;
    @Mock AuctionOperationsService operations;
    @Mock AuctionBidRepository bids;
    @Mock OrderRepository orders;
    @Mock AuctionDisputeService disputes;

    private SuperAdminAuctionController controller;
    private final Authentication auth = new UsernamePasswordAuthenticationToken("superadmin", "pass");

    @BeforeEach
    void setUp() {
        controller = new SuperAdminAuctionController(auctions, operations, bids, orders);
        ReflectionTestUtils.setField(controller, "disputes", disputes);
        lenient().when(auctions.adminAuctions(anyString())).thenReturn(List.of());
        lenient().when(orders.findAll()).thenReturn(List.of());
    }

    @Test
    void overview_filtersPaginatesAndCalculatesMetrics() {
        LocalDateTime created = LocalDateTime.of(2026, 11, 10, 10, 0);
        AuctionResponse completed = auction(1, "Completed card", "COMPLETED", created, 2, 1);
        AuctionResponse expired = auction(2, "Expired card", "CHECKOUT_EXPIRED", created.minusDays(1), 3, 2);
        AuctionResponse other = auction(3, "Other card", "LIVE", created.minusDays(10), 4, 3);
        when(auctions.adminAuctions("ALL")).thenReturn(List.of(completed, expired, other));
        AuctionBid bid = new AuctionBid();
        bid.setAmount(125d);
        when(bids.findByAuction_AuctionIdOrderByCreatedAtDesc(anyInt())).thenReturn(List.of(bid));
        Order paid = new Order();
        paid.setAuctionId(1);
        paid.setPaymentStatus("PAID");
        paid.setSubtotal(250d);
        when(orders.findAll()).thenReturn(List.of(paid));

        Map<String, Object> result = controller.overview(auth, "card", "ALL", null,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 11), 0, 2);

        assertEquals(2, result.get("total"));
        assertEquals(1L, result.get("expiredCount"));
        assertEquals(2L, result.get("sellThroughDenominator"));
        assertEquals(0.5d, result.get("sellThroughRate"));
        assertEquals(250d, result.get("gmv"));
        assertEquals(250d, result.get("bidValue"));
        assertEquals(2, ((List<?>) result.get("items")).size());
        verify(operations).superadmin("superadmin");
    }

    @Test
    void overview_supportsStoreQueryAndEmptySellThrough() {
        AuctionResponse row = auction(9, "Store card", "CANCELLED", LocalDateTime.now(), 7, 12);
        when(auctions.adminAuctions("LIVE")).thenReturn(List.of(row));
        Map<String, Object> result = controller.overview(auth, "Store", "LIVE", 7, null, null, 2, 20);
        assertEquals(0, result.get("total"));
        assertEquals(0d, result.get("sellThroughRate"));
        assertEquals(0L, result.get("sellThroughDenominator"));
    }

    @Test
    void overview_rejectsInvalidPagingAndDateRange() {
        assertThrows(ResponseStatusException.class, () -> controller.overview(auth, "", "ALL", null, null, null, -1, 20));
        assertThrows(ResponseStatusException.class, () -> controller.overview(auth, "", "ALL", null, null, null, 0, 0));
        assertThrows(ResponseStatusException.class, () -> controller.overview(auth, "", "ALL", null, null, null, 0, 101));
        assertThrows(ResponseStatusException.class, () -> controller.overview(auth, "", "ALL", null,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1), 0, 20));
    }

    @Test
    void delegatesSettingsAuditDisputesRestrictionsAndModeration() {
        AuctionSettings settings = new AuctionSettings();
        when(operations.settings()).thenReturn(settings);
        when(operations.updateSettings("superadmin", settings)).thenReturn(settings);
        assertSame(settings, controller.settings(auth));
        assertSame(settings, controller.settings(auth, settings));

        List<AuctionEvent> audit = List.of(new AuctionEvent());
        when(operations.auditTrail("superadmin", 5)).thenReturn(audit);
        assertSame(audit, controller.audit(auth, 5));

        List<AuctionEvent> disputeList = List.of(new AuctionEvent());
        when(disputes.list("superadmin")).thenReturn(disputeList);
        assertSame(disputeList, controller.disputes(auth));
        AuctionEvent resolved = new AuctionEvent();
        when(disputes.resolve("superadmin", 8L, "RESOLVED", "done")).thenReturn(resolved);
        assertSame(resolved, controller.resolve(auth, 8L, new SuperAdminAuctionController.Resolution("RESOLVED", "done")));

        AuctionBidderRestriction restriction = new AuctionBidderRestriction();
        when(operations.restrict("superadmin", 7, true, "fraud")).thenReturn(restriction);
        assertSame(restriction, controller.restrict(auth, 7, new SuperAdminAuctionController.Restriction(true, "fraud")));

        AuctionResponse moderated = auction(5, "Card", "SUSPENDED", LocalDateTime.now(), 1, 1);
        when(auctions.moderate("superadmin", 5, "SUSPEND", "fraud")).thenReturn(moderated);
        assertSame(moderated, controller.moderate(auth, 5, new SuperAdminAuctionController.Moderation("SUSPEND", "fraud")));
    }

    private AuctionResponse auction(int id, String title, String status, LocalDateTime created, int seller, int store) {
        return new AuctionResponse(id, title, "Pokemon", "Single", "image", "desc", 100d, 125d, 2,
                created.minusHours(1), created.plusDays(1), null, status, "MARKETPLACE", store,
                "Store " + store, seller, null, seller, null, created);
    }
}
