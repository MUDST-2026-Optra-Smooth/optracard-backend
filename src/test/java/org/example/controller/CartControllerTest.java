package org.example.controller;

import org.example.dto.CartResponse;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CartController cartController;

    private User user;
    private CartResponse mockCartResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUaId(7);
        user.setEmail("buyer@example.com");

        mockCartResponse = new CartResponse(1, List.of(), 0.0, 0);
    }

    private void mockAuthenticatedUser() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("buyer@example.com");
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void getCart_whenAuthorized_returnsCart() {
        mockAuthenticatedUser();
        when(cartService.getCartResponse(7)).thenReturn(mockCartResponse);

        ResponseEntity<CartResponse> response = cartController.getCart(7, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockCartResponse, response.getBody());
    }

    @Test
    void getCart_whenAccessingDifferentUserCart_throwsForbidden() {
        mockAuthenticatedUser();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartController.getCart(99, authentication));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getCart_whenNotAuthenticated_throwsUnauthorized() {
        when(authentication.isAuthenticated()).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartController.getCart(7, authentication));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void addItemToCart_delegatesToService() {
        mockAuthenticatedUser();
        when(cartService.getCartResponse(7)).thenReturn(mockCartResponse);

        ResponseEntity<CartResponse> response = cartController.addItemToCart(7, 10, 2, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).addItemToCart(7, 10, 2);
    }

    @Test
    void updateItemQuantity_delegatesToService() {
        mockAuthenticatedUser();
        when(cartService.getCartResponse(7)).thenReturn(mockCartResponse);

        ResponseEntity<CartResponse> response = cartController.updateItemQuantity(7, 10, 5, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).updateItemQuantity(7, 10, 5);
    }

    @Test
    void removeItem_delegatesToService() {
        mockAuthenticatedUser();
        when(cartService.getCartResponse(7)).thenReturn(mockCartResponse);

        ResponseEntity<CartResponse> response = cartController.removeItem(7, 10, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).removeItemFromCart(7, 10);
    }

    @Test
    void clearCart_delegatesToService() {
        mockAuthenticatedUser();
        when(cartService.getCartResponse(7)).thenReturn(mockCartResponse);

        ResponseEntity<CartResponse> response = cartController.clearCart(7, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).clearCart(7);
    }
}
