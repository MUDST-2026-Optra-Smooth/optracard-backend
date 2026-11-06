package org.example.controller;

import org.example.dto.MarketplaceStoreProfileResponse;
import org.example.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketplaceCatalogControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private MarketplaceCatalogController controller;

    @Test
    @DisplayName("store returns 200 OK when marketplace store profile is found")
    void store_found() {
        MarketplaceStoreProfileResponse profile = mock(MarketplaceStoreProfileResponse.class);
        when(productService.getMarketplaceStoreProfile(12)).thenReturn(Optional.of(profile));

        ResponseEntity<MarketplaceStoreProfileResponse> response = controller.store(12);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(profile, response.getBody());
        verify(productService).getMarketplaceStoreProfile(12);
    }

    @Test
    @DisplayName("store returns 404 NOT FOUND when store profile does not exist")
    void store_notFound() {
        when(productService.getMarketplaceStoreProfile(999)).thenReturn(Optional.empty());

        ResponseEntity<MarketplaceStoreProfileResponse> response = controller.store(999);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(productService).getMarketplaceStoreProfile(999);
    }
}

