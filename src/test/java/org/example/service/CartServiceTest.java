package org.example.service;

import org.example.model.Cart;
import org.example.model.Product;
import org.example.repository.CartRepository;
import org.example.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private Product product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setProId(100);
        product.setProName("Five-card stock item");
        product.setProQuantity(5);
        product.setIsActive(true);
        product.setListingSource("OFFICIAL");

        cart = new Cart();
        cart.setUserId(7);

        when(productRepository.findById(100)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(7)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void addItemAllowsAvailableStockButRejectsTheSixthItem() {
        cartService.addItemToCart(7, 100, 5);

        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).getQuantity());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cartService.addItemToCart(7, 100, 1)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Only 5 items of Five-card stock item are available in stock.", exception.getReason());
        assertEquals(5, cart.getItems().get(0).getQuantity());
    }
}
