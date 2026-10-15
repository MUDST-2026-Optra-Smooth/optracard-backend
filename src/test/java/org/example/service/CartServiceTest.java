package org.example.service;

import org.example.dto.CartResponse;
import org.example.model.Cart;
import org.example.model.CartItem;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        product.setProPriceOfSell(50.0);
        product.setIsActive(true);
        product.setListingSource("OFFICIAL");

        cart = new Cart();
        cart.setCartId(1);
        cart.setUserId(7);

        lenient().when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void addItemAllowsAvailableStockButRejectsTheSixthItem() {
        when(productRepository.findById(100)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(7)).thenReturn(Optional.of(cart));

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

    @Test
    void addItemToCart_withInvalidQuantity_throwsBadRequest() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cartService.addItemToCart(7, 100, 0)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateItemQuantity_updatesExistingItem() {
        when(productRepository.findById(100)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(7)).thenReturn(Optional.of(cart));

        CartItem item = new CartItem();
        item.setProductId(100);
        item.setQuantity(2);
        item.setCart(cart);
        cart.getItems().add(item);

        cartService.updateItemQuantity(7, 100, 4);

        assertEquals(1, cart.getItems().size());
        assertEquals(4, cart.getItems().get(0).getQuantity());
    }

    @Test
    void updateItemQuantity_whenZero_removesItem() {
        when(cartRepository.findByUserId(7)).thenReturn(Optional.of(cart));

        CartItem item = new CartItem();
        item.setProductId(100);
        item.setQuantity(2);
        item.setCart(cart);
        cart.getItems().add(item);

        cartService.updateItemQuantity(7, 100, 0);

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void removeItemFromCart_removesSpecificProduct() {
        when(cartRepository.findByUserId(7)).thenReturn(Optional.of(cart));

        CartItem item = new CartItem();
        item.setProductId(100);
        item.setQuantity(1);
        item.setCart(cart);
        cart.getItems().add(item);

        cartService.removeItemFromCart(7, 100);

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void clearCart_emptiesCart() {
        when(cartRepository.findByUserId(7)).thenReturn(Optional.of(cart));

        CartItem item = new CartItem();
        item.setProductId(100);
        item.setQuantity(3);
        item.setCart(cart);
        cart.getItems().add(item);

        cartService.clearCart(7);

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void getCartResponse_calculatesSubtotalAndCount() {
        when(cartRepository.findByUserId(7)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100)).thenReturn(Optional.of(product));

        CartItem item = new CartItem();
        item.setProductId(100);
        item.setQuantity(2);
        item.setCart(cart);
        cart.getItems().add(item);

        CartResponse response = cartService.getCartResponse(7);

        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals(100.0, response.subtotal());
        assertEquals(2, response.totalItemCount());
    }
}
