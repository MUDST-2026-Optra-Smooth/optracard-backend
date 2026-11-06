package org.example.controller;

import org.example.dto.CardGameOptionResponse;
import org.example.dto.SellerProductRequest;
import org.example.dto.SellerProductResponse;
import org.example.service.SellerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerControllerTest {

    @Mock
    private SellerService sellerService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SellerController sellerController;

    @Test
    @DisplayName("products() delegates to SellerService")
    void products_returnsSellerProductList() {
        when(authentication.getName()).thenReturn("seller@example.com");
        SellerProductResponse mockProd = new SellerProductResponse(
                1, "Charizard", "Pokemon", "Single", 100.0, 200.0, 5, "URL", "Set", "EN", "Desc", "PENDING", true, 1, "Store A"
        );
        when(sellerService.getProducts("seller@example.com")).thenReturn(List.of(mockProd));

        List<SellerProductResponse> response = sellerController.products(authentication);

        assertEquals(1, response.size());
        assertEquals("Charizard", response.get(0).name());
        verify(sellerService).getProducts("seller@example.com");
    }

    @Test
    @DisplayName("order() returns one order of the seller's store")
    void order_returnsTheRequestedOrder() {
        when(authentication.getName()).thenReturn("seller@example.com");
        org.example.dto.SellerOrderResponse order = new org.example.dto.SellerOrderResponse(
                44, "ORD-44", 30, null, 250.0, "Processing", "PAID", "standard", "Address", "John", "0812345678", null,
                List.of(), "Store A", 50.0, 200.0, null, null, 0.0
        );
        when(sellerService.getOrder("seller@example.com", 44)).thenReturn(order);

        org.example.dto.SellerOrderResponse response = sellerController.order(authentication, 44);

        assertEquals("ORD-44", response.orderNumber());
        verify(sellerService).getOrder("seller@example.com", 44);
    }

    @Test
    @DisplayName("games() returns available games")
    void games_returnsAvailableGames() {
        CardGameOptionResponse game = new CardGameOptionResponse(1, "Pokemon");
        when(sellerService.getCardGames()).thenReturn(List.of(game));

        List<CardGameOptionResponse> response = sellerController.games();

        assertEquals(1, response.size());
        assertEquals("Pokemon", response.get(0).name());
        verify(sellerService).getCardGames();
    }

    @Test
    @DisplayName("create() creates product and returns 201 Created")
    void create_createsProduct() {
        when(authentication.getName()).thenReturn("seller@example.com");
        SellerProductRequest req = new SellerProductRequest("Charizard", "Pokemon", "Single", 100.0, 200.0, 5, "URL", "Set", "EN", "Desc", null);
        SellerProductResponse res = new SellerProductResponse(
                1, "Charizard", "Pokemon", "Single", 100.0, 200.0, 5, "URL", "Set", "EN", "Desc", "PENDING", true, 1, "Store A"
        );
        when(sellerService.createProduct("seller@example.com", req)).thenReturn(res);

        ResponseEntity<SellerProductResponse> response = sellerController.create(authentication, req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Charizard", response.getBody().name());
        verify(sellerService).createProduct("seller@example.com", req);
    }

    @Test
    @DisplayName("delete() deletes seller product and returns 204 No Content")
    void delete_returnsNoContent() {
        when(authentication.getName()).thenReturn("seller@example.com");
        doNothing().when(sellerService).deleteProduct("seller@example.com", 1);

        ResponseEntity<Void> response = sellerController.delete(authentication, 1);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(sellerService).deleteProduct("seller@example.com", 1);
    }
}

