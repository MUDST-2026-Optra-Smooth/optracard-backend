package org.example.service;

import org.example.model.MarketplaceFeeConfig;
import org.example.model.MarketplaceSettlementLedger;
import org.example.model.MarketplaceStore;
import org.example.model.Order;
import org.example.model.SettlementEntryType;
import org.example.repository.MarketplaceSettlementLedgerRepository;
import org.example.repository.MarketplaceStoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceSettlementServiceTest {
    @Mock private MarketplaceSettlementLedgerRepository ledgerRepository;
    @Mock private MarketplaceStoreRepository storeRepository;
    @Mock private MarketplaceFeeService feeService;

    private MarketplaceSettlementService service;

    @BeforeEach
    void setUp() {
        service = new MarketplaceSettlementService(ledgerRepository, storeRepository, feeService);
    }

    @Test
    void calculatesFivePercentFromDiscountedItemsAndNeverFromShipping() {
        Order order = marketplaceOrder(41, 1_000d, 100d, 50d);
        stubLedgerSave();
        when(ledgerRepository.findByEntryTypeAndReferenceTypeAndReferenceId(
                SettlementEntryType.ORDER_CHARGE, "ORDER", "41")).thenReturn(Optional.empty());
        when(storeRepository.findById(9)).thenReturn(Optional.of(store()));
        when(feeService.currentConfiguration()).thenReturn(feeConfig("5.00"));

        service.recordOrderCharge(order);

        ArgumentCaptor<MarketplaceSettlementLedger> captor = ArgumentCaptor.forClass(MarketplaceSettlementLedger.class);
        verify(ledgerRepository).saveAndFlush(captor.capture());
        MarketplaceSettlementLedger entry = captor.getValue();
        assertEquals(new BigDecimal("900.00"), entry.getGrossAmount());
        assertEquals(new BigDecimal("5.00"), entry.getFeeRate());
        assertEquals(new BigDecimal("45.00"), entry.getFeeAmount());
        assertEquals(new BigDecimal("855.00"), entry.getNetAmount());
    }

    @Test
    void officialStoreOrderNeverCreatesSettlementLedger() {
        Order officialOrder = marketplaceOrder(42, 500d, 0d, 50d);
        officialOrder.setSource("OFFICIAL");
        officialOrder.setStoreId(null);

        assertTrue(service.recordOrderCharge(officialOrder).isEmpty());
        verify(ledgerRepository, never()).saveAndFlush(any());
        verify(feeService, never()).currentConfiguration();
    }

    @Test
    void laterFeeRateDoesNotChangeAnExistingOrderSnapshot() {
        Order firstOrder = marketplaceOrder(43, 1_000d, 0d, 0d);
        Order secondOrder = marketplaceOrder(44, 1_000d, 0d, 0d);
        stubLedgerSave();
        when(ledgerRepository.findByEntryTypeAndReferenceTypeAndReferenceId(any(), any(), any())).thenReturn(Optional.empty());
        when(storeRepository.findById(9)).thenReturn(Optional.of(store()));
        when(feeService.currentConfiguration()).thenReturn(feeConfig("5.00"), feeConfig("7.00"));

        MarketplaceSettlementLedger first = service.recordOrderCharge(firstOrder).orElseThrow();
        MarketplaceSettlementLedger second = service.recordOrderCharge(secondOrder).orElseThrow();

        assertEquals(new BigDecimal("5.00"), first.getFeeRate());
        assertEquals(new BigDecimal("50.00"), first.getFeeAmount());
        assertEquals(new BigDecimal("7.00"), second.getFeeRate());
        assertEquals(new BigDecimal("70.00"), second.getFeeAmount());
    }

    @Test
    void cancellationCreatesOneNegativeReversalWithoutDeletingOriginalCharge() {
        Order order = marketplaceOrder(45, 1_000d, 100d, 0d);
        stubLedgerSave();
        MarketplaceSettlementLedger charge = new MarketplaceSettlementLedger();
        charge.setId(100);
        charge.setEntryType(SettlementEntryType.ORDER_CHARGE);
        charge.setStoreId(9);
        charge.setStoreName("Card Corner");
        charge.setSellerUserId(7);
        charge.setReferenceNumber("ORD-20261110-0045");
        charge.setGrossAmount(new BigDecimal("900.00"));
        charge.setFeeRate(new BigDecimal("5.00"));
        charge.setFeeAmount(new BigDecimal("45.00"));
        charge.setNetAmount(new BigDecimal("855.00"));
        when(ledgerRepository.findByEntryTypeAndReferenceTypeAndReferenceId(
                SettlementEntryType.ORDER_CHARGE, "ORDER", "45")).thenReturn(Optional.of(charge));
        when(ledgerRepository.findByReversalOfId(100)).thenReturn(Optional.empty());

        MarketplaceSettlementLedger reversal = service.recordOrderReversal(order).orElseThrow();

        assertEquals(SettlementEntryType.REVERSAL, reversal.getEntryType());
        assertEquals(new BigDecimal("-900.00"), reversal.getGrossAmount());
        assertEquals(new BigDecimal("-45.00"), reversal.getFeeAmount());
        assertEquals(new BigDecimal("-855.00"), reversal.getNetAmount());
        assertEquals(100, reversal.getReversalOfId());
    }

    private void stubLedgerSave() {
        when(ledgerRepository.saveAndFlush(any(MarketplaceSettlementLedger.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private MarketplaceStore store() {
        MarketplaceStore store = new MarketplaceStore();
        store.setStoreId(9);
        store.setSellerUserId(7);
        store.setStoreName("Card Corner");
        return store;
    }

    private MarketplaceFeeConfig feeConfig(String rate) {
        MarketplaceFeeConfig config = new MarketplaceFeeConfig();
        config.setFeeRate(new BigDecimal(rate));
        return config;
    }

    private Order marketplaceOrder(Integer id, double subtotal, double discount, double shipping) {
        Order order = new Order();
        order.setOrderId(id);
        order.setStoreId(9);
        order.setStoreName("Card Corner");
        order.setSource("MARKETPLACE");
        order.setPaymentStatus("PAID");
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setShippingFee(shipping);
        order.setTotalPrice(subtotal - discount + shipping);
        return order;
    }
}
