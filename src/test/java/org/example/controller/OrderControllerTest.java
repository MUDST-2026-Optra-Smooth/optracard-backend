package org.example.controller;

import org.example.dto.CheckoutResponse;
import org.example.dto.CreateOrderRequest;
import org.example.dto.OrderResponse;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OrderController orderController;

    private User testUser;
    private OrderResponse mockOrder;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUaId(10);
        testUser.setEmail("user@example.com");

        mockOrder = new OrderResponse(
                1, "ORD-001", null, null, "OFFICIAL", LocalDateTime.now(),
                100.0, "COD", "PENDING", "PENDING", "Standard", 50.0,
                null, "Address", "John", "0812345678", List.of(),
                50.0, null, null, 0.0
        );
    }

    @Test
    @DisplayName("history() returns order history for authenticated user")
    void history_returnsOrderList() {
        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrderHistory(10)).thenReturn(List.of(mockOrder));

        ResponseEntity<List<OrderResponse>> response = orderController.history(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("ORD-001", response.getBody().get(0).orderNumber());
    }

    @Test
    @DisplayName("detail() returns order details for specific order number")
    void detail_returnsOrderResponse() {
        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(orderService.getOrderDetail(10, "ORD-001")).thenReturn(mockOrder);

        ResponseEntity<OrderResponse> response = orderController.detail(authentication, "ORD-001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ORD-001", response.getBody().orderNumber());
    }

    @Test
    @DisplayName("placeOrder() places order and returns checkout response")
    void placeOrder_returnsCheckoutResponse() {
        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        CreateOrderRequest request = new CreateOrderRequest("John", "0812345678", "Address", "COD", "Note");
        CheckoutResponse checkoutRes = new CheckoutResponse(List.of(mockOrder), 1, 150.0);
        when(orderService.placeOrder(10, request)).thenReturn(checkoutRes);

        ResponseEntity<CheckoutResponse> response = orderController.placeOrder(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().orderCount());
        assertEquals(150.0, response.getBody().total());
    }

    @Test
    @DisplayName("currentUser throws exception when authenticated user is not found in database")
    void currentUser_whenNotFound_throwsIllegalStateException() {
        when(authentication.getName()).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> orderController.history(authentication));
    }
}

