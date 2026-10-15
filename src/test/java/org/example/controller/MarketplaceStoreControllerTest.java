package org.example.controller;

import org.example.dto.StoreApplicationRequest;
import org.example.dto.StoreApplicationResponse;
import org.example.service.MarketplaceStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketplaceStoreControllerTest {

    @Mock
    private MarketplaceStoreService storeService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MarketplaceStoreController controller;

    private StoreApplicationResponse mockResponse;
    private StoreApplicationRequest mockRequest;

    @BeforeEach
    void setUp() {
        when(authentication.getName()).thenReturn("merchant@example.com");

        mockResponse = new StoreApplicationResponse(
                1, "card-hub", "PENDING", "Card Hub", "TCG shop",
                false, "John", "Doe", "john@example.com", "0812345678",
                "SCB", "Main", "John Doe", "123-456", "Street",
                "Bangkok", "District", "Subdistrict", "10110",
                null, null, true, LocalDateTime.now(), null, null
        );

        mockRequest = new StoreApplicationRequest(
                "Card Hub", "TCG shop", false, "John", "Doe", "john@example.com",
                "0812345678", "SCB", "Main", "John Doe", "123-456", "Street",
                "Bangkok", "District", "Subdistrict", "10110", null, null, true
        );
    }

    @Test
    void getMyStore_whenExists_returnsOk() {
        when(storeService.getMyStore("merchant@example.com")).thenReturn(mockResponse);

        ResponseEntity<StoreApplicationResponse> response = controller.getMyStore(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    void getMyStore_whenNotExists_returnsNoContent() {
        when(storeService.getMyStore("merchant@example.com")).thenReturn(null);

        ResponseEntity<StoreApplicationResponse> response = controller.getMyStore(authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void submit_returnsCreatedStatus() {
        when(storeService.saveApplication("merchant@example.com", mockRequest)).thenReturn(mockResponse);

        ResponseEntity<StoreApplicationResponse> response = controller.submit(authentication, mockRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    void update_returnsOkStatus() {
        when(storeService.saveApplication("merchant@example.com", mockRequest)).thenReturn(mockResponse);

        ResponseEntity<StoreApplicationResponse> response = controller.update(authentication, mockRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }
}
