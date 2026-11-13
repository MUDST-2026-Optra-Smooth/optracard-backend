package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.*;
import org.example.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionOperationsServiceTest {
    @Mock private AuctionBidderRestrictionRepository restrictions;
    @Mock private AuctionSettingsRepository settings;
    @Mock private AuctionEventRepository events;
    @Mock private UserRepository users;
    private AuctionOperationsService service;

    @BeforeEach
    void setUp() {
        service = new AuctionOperationsService(settings, events, users, new ObjectMapper());
        org.springframework.test.util.ReflectionTestUtils.setField(service, "restrictions", restrictions);
    }

    private User admin() {
        User user = new User(); user.setUaId(1); user.setEmail("admin@test"); user.setRole("SUPER_ADMIN"); return user;
    }

    @Test
    void settingsCanBeReadAndUpdatedBySuperAdmin() {
        when(users.findByEmail("admin@test")).thenReturn(Optional.of(admin()));
        when(settings.findById(1)).thenReturn(Optional.empty());
        AuctionSettings input = new AuctionSettings();
        input.setBidIncrement(75d); input.setCheckoutHours(24); input.setAntiSnipingMinutes(5);
        input.setMinDurationHours(1); input.setMaxDurationHours(168);
        when(settings.save(any(AuctionSettings.class))).thenAnswer(inv -> inv.getArgument(0));
        AuctionSettings result = service.updateSettings("admin@test", input);
        assertEquals(75d, result.getBidIncrement());
        verify(events).save(any(AuctionEvent.class));
    }

    @Test
    void invalidSettingsAndUnauthorizedActorsAreRejected() {
        when(users.findByEmail("admin@test")).thenReturn(Optional.of(admin()));
        AuctionSettings invalid = new AuctionSettings();
        invalid.setBidIncrement(0d); invalid.setCheckoutHours(24); invalid.setAntiSnipingMinutes(0);
        invalid.setMinDurationHours(1); invalid.setMaxDurationHours(168);
        ResponseStatusException bad = assertThrows(ResponseStatusException.class, () -> service.updateSettings("admin@test", invalid));
        assertEquals(HttpStatus.BAD_REQUEST, bad.getStatusCode());
        User seller = new User(); seller.setUaId(2); seller.setRole("SELLER");
        when(users.findByEmail("seller@test")).thenReturn(Optional.of(seller));
        when(settings.findById(1)).thenReturn(Optional.empty());
        assertNotNull(service.settings());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> service.superadmin("seller@test")).getStatusCode());
    }

    @Test
    void restrictionAndBidderChecksCoverCreateUpdateAndBannedPaths() {
        when(users.findByEmail("admin@test")).thenReturn(Optional.of(admin()));
        when(users.existsById(7)).thenReturn(true);
        when(restrictions.findById(7)).thenReturn(Optional.empty());
        when(restrictions.save(any(AuctionBidderRestriction.class))).thenAnswer(inv -> inv.getArgument(0));
        AuctionBidderRestriction result = service.restrict("admin@test", 7, true, "fraud");
        assertTrue(result.isBanned());
        verify(events).save(any(AuctionEvent.class));
        when(restrictions.findById(7)).thenReturn(Optional.of(result));
        assertThrows(ResponseStatusException.class, () -> service.requireAllowedBidder(7));
        when(restrictions.findById(8)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.requireAllowedBidder(8));
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.restrict("admin@test", 7, false, "")).getStatusCode());
        when(users.existsById(9)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.restrict("admin@test", 9, false, "ok")).getStatusCode());
    }

    @Test
    void notificationsAuditAndReadEnforceOwnershipAndDedupe() {
        User user = admin();
        when(users.findByEmail("admin@test")).thenReturn(Optional.of(user));
        Auction auction = new Auction(); auction.setAuctionId(4); auction.setTitle("Demo");
        when(events.existsByDedupeKey("key")).thenReturn(false);
        service.notify(7, 4, "BID", "message", "key");
        verify(events).save(any(AuctionEvent.class));
        when(events.existsByDedupeKey("key")).thenReturn(true);
        service.notify(7, 4, "BID", "message", "key");
        when(events.findByRecipientIdOrderByCreatedAtDesc(1)).thenReturn(List.of());
        assertTrue(service.notifications("admin@test").isEmpty());
        AuctionEvent event = new AuctionEvent(); event.setRecipientId(1); event.setAuctionId(4); event.setKind("AUDIT");
        when(events.findByIdForUpdate(9L)).thenReturn(Optional.of(event));
        service.read("admin@test", 9L);
        verify(events, atLeastOnce()).save(event);
        event.setRecipientId(2);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class, () -> service.read("admin@test", 9L)).getStatusCode());
        when(events.findByAuctionIdOrderByCreatedAtDesc(4)).thenReturn(List.of(event));
        assertEquals(1, service.auditTrail("admin@test", 4).size());
    }
}
