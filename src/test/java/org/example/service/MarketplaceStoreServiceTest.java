package org.example.service;

import org.example.dto.StoreApplicationRequest;
import org.example.dto.StoreApplicationResponse;
import org.example.model.MarketplaceStore;
import org.example.model.User;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketplaceStoreServiceTest {

    @Mock
    private MarketplaceStoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    private MarketplaceStoreService storeService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        storeService = new MarketplaceStoreService(storeRepository, userRepository);
        sampleUser = new User();
        sampleUser.setUaId(10);
        sampleUser.setEmail("seller@optracard.com");
        sampleUser.setRole("USER");
    }

    private StoreApplicationRequest createValidRequest() {
        return new StoreApplicationRequest(
                "My Poke Shop",
                "Best pokemon cards in town",
                true,
                "Ash",
                "Ketchum",
                "ash@pokemon.com",
                "0812345678",
                "Bangkok Bank",
                "Siam",
                "Ash Ketchum",
                "123-4-56789-0",
                "123 Pallet Town Road",
                "Bangkok",
                "Pathum Wan",
                "Siam",
                "10330",
                "https://example.com/logo.jpg",
                "https://example.com/book.jpg",
                true
        );
    }

    @Test
    void getMyStore_whenStoreExists_returnsResponse() {
        when(userRepository.findByEmail("seller@optracard.com")).thenReturn(Optional.of(sampleUser));
        MarketplaceStore store = new MarketplaceStore();
        store.setStoreId(1);
        store.setStoreName("My Poke Shop");
        store.setStoreStatus("APPROVED");
        when(storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(10)).thenReturn(Optional.of(store));

        StoreApplicationResponse response = storeService.getMyStore("seller@optracard.com");

        assertNotNull(response);
        assertEquals(1, response.storeId());
        assertEquals("My Poke Shop", response.storeName());
        assertEquals("APPROVED", response.storeStatus());
    }

    @Test
    void getMyStore_whenStoreDoesNotExist_returnsNull() {
        when(userRepository.findByEmail("seller@optracard.com")).thenReturn(Optional.of(sampleUser));
        when(storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(10)).thenReturn(Optional.empty());

        StoreApplicationResponse response = storeService.getMyStore("seller@optracard.com");

        assertNull(response);
    }

    @Test
    void saveApplication_withNullRequest_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> storeService.saveApplication("seller@optracard.com", null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void saveApplication_whenTermsNotAccepted_throwsBadRequest() {
        when(userRepository.findByEmail("seller@optracard.com")).thenReturn(Optional.of(sampleUser));
        StoreApplicationRequest req = new StoreApplicationRequest(
                "My Shop", "Description", false, "Ash", "Ketchum",
                "ash@pokemon.com", "0812345678", "SCB", "Main", "Ash K",
                "111-222", "Address", "Province", "District", "Subdistrict",
                "10000", null, null, false
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> storeService.saveApplication("seller@optracard.com", req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Terms and conditions"));
    }

    @Test
    void saveApplication_withValidRequest_savesStoreWithPendingStatus() {
        when(userRepository.findByEmail("seller@optracard.com")).thenReturn(Optional.of(sampleUser));
        when(storeRepository.findFirstBySellerUserIdOrderByStoreIdDesc(10)).thenReturn(Optional.empty());

        StoreApplicationRequest req = createValidRequest();
        when(storeRepository.saveAndFlush(any(MarketplaceStore.class))).thenAnswer(inv -> inv.getArgument(0));

        StoreApplicationResponse response = storeService.saveApplication("seller@optracard.com", req);

        assertNotNull(response);
        assertEquals("My Poke Shop", response.storeName());
        assertEquals("PENDING", response.storeStatus());
        assertTrue(response.storeSlug().contains("my-poke-shop-10"));

        ArgumentCaptor<MarketplaceStore> captor = ArgumentCaptor.forClass(MarketplaceStore.class);
        verify(storeRepository).saveAndFlush(captor.capture());
        assertEquals(10, captor.getValue().getSellerUserId());
        assertEquals("PENDING", captor.getValue().getStoreStatus());
    }

    @Test
    void reviewApplication_approvesStoreAndPromotesUserToSeller() {
        MarketplaceStore store = new MarketplaceStore();
        store.setStoreId(5);
        store.setSellerUserId(10);
        store.setStoreStatus("PENDING");

        when(storeRepository.findByStoreId(5)).thenReturn(Optional.of(store));
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleUser));
        when(storeRepository.saveAndFlush(any(MarketplaceStore.class))).thenAnswer(inv -> inv.getArgument(0));

        StoreApplicationResponse response = storeService.reviewApplication(5, "APPROVED", "Approved by admin");

        assertEquals("APPROVED", response.storeStatus());
        assertEquals("Approved by admin", response.reviewNote());
        assertEquals("SELLER", sampleUser.getRole());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void reviewApplication_rejectsStoreAndDemotesSellerRole() {
        sampleUser.setRole("SELLER");
        MarketplaceStore store = new MarketplaceStore();
        store.setStoreId(5);
        store.setSellerUserId(10);
        store.setStoreStatus("PENDING");

        when(storeRepository.findByStoreId(5)).thenReturn(Optional.of(store));
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleUser));
        when(storeRepository.saveAndFlush(any(MarketplaceStore.class))).thenAnswer(inv -> inv.getArgument(0));

        StoreApplicationResponse response = storeService.reviewApplication(5, "REJECTED", "Incomplete docs");

        assertEquals("REJECTED", response.storeStatus());
        assertEquals("Incomplete docs", response.reviewNote());
        assertEquals("USER", sampleUser.getRole());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void reviewApplication_withInvalidStatus_throwsBadRequest() {
        MarketplaceStore store = new MarketplaceStore();
        store.setStoreId(5);
        when(storeRepository.findByStoreId(5)).thenReturn(Optional.of(store));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> storeService.reviewApplication(5, "INVALID_STATUS", null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void reviewApplication_whenNotFound_throwsNotFound() {
        when(storeRepository.findByStoreId(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> storeService.reviewApplication(999, "APPROVED", null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
