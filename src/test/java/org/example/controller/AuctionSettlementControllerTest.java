package org.example.controller;

import org.example.dto.AuctionBidRequest;
import org.example.dto.AuctionBidResponse;
import org.example.dto.AuctionRequest;
import org.example.dto.AuctionResponse;
import org.example.dto.MarketplaceFeeConfigResponse;
import org.example.dto.SettlementReportResponse;
import org.example.dto.UpdateMarketplaceFeeRequest;
import org.example.service.AuctionService;
import org.example.service.MarketplaceFeeService;
import org.example.service.MarketplaceSettlementReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionSettlementControllerTest {
    @Mock private AuctionService auctionService;
    @Mock private MarketplaceFeeService feeService;
    @Mock private MarketplaceSettlementReportService reportService;
    @Mock private Authentication authentication;

    @Test
    void publicAuctionEndpointsDelegateFiltersDetailsAndBidHistory() {
        AuctionController controller = new AuctionController(auctionService);
        AuctionResponse auction = org.mockito.Mockito.mock(AuctionResponse.class);
        AuctionBidResponse bid = org.mockito.Mockito.mock(AuctionBidResponse.class);
        when(auctionService.listPublic("Pokemon", "OFFICIAL", "LIVE")).thenReturn(List.of(auction));
        when(auctionService.getPublic(7)).thenReturn(auction);
        when(auctionService.bidHistory(7)).thenReturn(List.of(bid));

        assertEquals(List.of(auction), controller.list("Pokemon", "OFFICIAL", "LIVE"));
        assertSame(auction, controller.detail(7));
        assertEquals(List.of(bid), controller.bids(7));
        verify(auctionService).listPublic("Pokemon", "OFFICIAL", "LIVE");
    }

    @Test
    void authenticatedAuctionMutationsForwardIdentityAndPayload() {
        AuctionController controller = new AuctionController(auctionService);
        AuctionBidRequest request = new AuctionBidRequest(1250d);
        AuctionBidResponse response = org.mockito.Mockito.mock(AuctionBidResponse.class);
        when(authentication.getName()).thenReturn("buyer@example.test");
        when(auctionService.placeBid("buyer@example.test", 7, request)).thenReturn(response);

        assertSame(response, controller.bid(authentication, 7, request));
        verify(auctionService).placeBid("buyer@example.test", 7, request);
    }

    @Test
    void sellerAndAdminControllersReturnCreatedAndForwardReviewPayloads() {
        SellerAuctionController seller = new SellerAuctionController(auctionService);
        AdminAuctionController admin = new AdminAuctionController(auctionService);
        AuctionRequest request = org.mockito.Mockito.mock(AuctionRequest.class);
        AuctionResponse created = org.mockito.Mockito.mock(AuctionResponse.class);
        when(authentication.getName()).thenReturn("seller@example.test");
        when(auctionService.createSellerAuction("seller@example.test", request)).thenReturn(created);

        assertEquals(HttpStatus.CREATED, seller.create(authentication, request).getStatusCode());
        verify(auctionService).createSellerAuction("seller@example.test", request);

        when(authentication.getName()).thenReturn("admin@example.test");
        when(auctionService.reviewAuction("admin@example.test", 8, false, "unclear photo")).thenReturn(created);
        assertSame(created, admin.review(authentication, 8, new AdminAuctionController.ReviewRequest(false, "unclear photo")));
        verify(auctionService).reviewAuction("admin@example.test", 8, false, "unclear photo");
    }

    @Test
    void settlementControllersPreserveFiltersAndFeeVersionPayload() {
        SellerSettlementController seller = new SellerSettlementController(reportService);
        SuperAdminSettlementController superAdmin = new SuperAdminSettlementController(reportService);
        MarketplaceFeeController fee = new MarketplaceFeeController(feeService);
        SettlementReportResponse report = org.mockito.Mockito.mock(SettlementReportResponse.class);
        MarketplaceFeeConfigResponse config = org.mockito.Mockito.mock(MarketplaceFeeConfigResponse.class);
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 10);
        when(authentication.getName()).thenReturn("seller@example.test");
        when(reportService.sellerReport("seller@example.test", from, to, "AUCTION_CHARGE")).thenReturn(report);
        when(reportService.superAdminReport(from, to, "ORDER_CHARGE", 7, 9)).thenReturn(report);
        when(feeService.update(new UpdateMarketplaceFeeRequest(new BigDecimal("6.25")), "seller@example.test")).thenReturn(config);

        assertSame(report, seller.report(authentication, from, to, "AUCTION_CHARGE"));
        assertSame(report, superAdmin.report(from, to, "ORDER_CHARGE", 7, 9));
        assertSame(config, fee.updateConfiguration(authentication, new UpdateMarketplaceFeeRequest(new BigDecimal("6.25"))));
    }
}
