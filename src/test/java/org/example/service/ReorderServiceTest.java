package org.example.service;

import org.example.dto.ReorderResponse;
import org.example.model.Cart;
import org.example.model.CartItem;
import org.example.model.Order;
import org.example.model.OrderItem;
import org.example.model.Product;
import org.example.repository.OrderRepository;
import org.example.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReorderServiceTest {
    private static final int USER_ID = 10;
    private static final String ORDER_NUMBER = "ORD-20260101-0001";

    @Mock private OrderService orderService;
    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CartService cartService;

    private ReorderService reorderService;
    private Cart cart;
    private Order order;

    @BeforeEach
    void setUp() {
        reorderService = new ReorderService(orderService, orderRepository, productRepository, cartService);
        cart = new Cart();
        cart.setUserId(USER_ID);
        cart.setItems(new ArrayList<>());
        order = new Order();
        order.setOrderId(1);
        order.setItems(new ArrayList<>());
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(order));
        when(orderService.orderNumber(order)).thenReturn(ORDER_NUMBER);
        when(cartService.getCartByUserId(USER_ID)).thenReturn(cart);
    }

    private void orderLine(int productId, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setPrice(100d);
        order.getItems().add(item);
    }

    private Product product(int id, String name, int stock, boolean purchasable) {
        Product product = new Product();
        product.setProId(id);
        product.setProName(name);
        product.setProQuantity(stock);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(cartService.isPurchasable(product)).thenReturn(purchasable);
        return product;
    }

    @Test
    @DisplayName("adds an active, in-stock item with its original quantity")
    void addsAvailableItem() {
        orderLine(1, 2);
        product(1, "Charizard", 5, true);

        ReorderResponse result = reorderService.reorder(USER_ID, ORDER_NUMBER);

        verify(cartService).addItemToCart(USER_ID, 1, 2);
        assertEquals(1, result.addedCount());
        assertEquals(0, result.skippedCount());
        assertEquals(2, result.added().get(0).addedQuantity());
        assertNull(result.added().get(0).reason());
    }

    @Test
    @DisplayName("reports a deleted product as unavailable instead of adding it")
    void reportsDeletedProduct() {
        orderLine(7, 1);
        when(productRepository.findById(7)).thenReturn(Optional.empty());

        ReorderResponse result = reorderService.reorder(USER_ID, ORDER_NUMBER);

        verify(cartService, never()).addItemToCart(anyInt(), anyInt(), anyInt());
        assertEquals(0, result.addedCount());
        assertEquals(1, result.skippedCount());
        assertEquals(ReorderResponse.UNAVAILABLE, result.skipped().get(0).reason());
        assertEquals(7, result.skipped().get(0).productId());
    }

    @Test
    @DisplayName("reports an inactive or unapproved product as unavailable")
    void reportsInactiveProduct() {
        orderLine(2, 1);
        product(2, "Pikachu", 5, false);

        ReorderResponse result = reorderService.reorder(USER_ID, ORDER_NUMBER);

        verify(cartService, never()).addItemToCart(anyInt(), anyInt(), anyInt());
        assertEquals(ReorderResponse.UNAVAILABLE, result.skipped().get(0).reason());
        assertEquals("Pikachu", result.skipped().get(0).name());
    }

    @Test
    @DisplayName("reports an out-of-stock product instead of adding it")
    void reportsOutOfStock() {
        orderLine(3, 1);
        product(3, "Blastoise", 0, true);

        ReorderResponse result = reorderService.reorder(USER_ID, ORDER_NUMBER);

        verify(cartService, never()).addItemToCart(anyInt(), anyInt(), anyInt());
        assertEquals(ReorderResponse.OUT_OF_STOCK, result.skipped().get(0).reason());
    }

    @Test
    @DisplayName("adds only the available quantity and says so when stock is lower than ordered")
    void limitsToAvailableStock() {
        orderLine(4, 5);
        product(4, "Booster Box", 2, true);

        ReorderResponse result = reorderService.reorder(USER_ID, ORDER_NUMBER);

        verify(cartService).addItemToCart(USER_ID, 4, 2);
        assertEquals(1, result.addedCount());
        assertEquals(0, result.skippedCount());
        ReorderResponse.Line line = result.added().get(0);
        assertEquals(5, line.requestedQuantity());
        assertEquals(2, line.addedQuantity());
        assertEquals(ReorderResponse.LIMITED_STOCK, line.reason());
    }

    @Test
    @DisplayName("counts what is already in the cart against the stock")
    void respectsQuantityAlreadyInCart() {
        orderLine(5, 3);
        product(5, "Sleeves", 4, true);
        CartItem existing = new CartItem();
        existing.setProductId(5);
        existing.setQuantity(3);
        cart.getItems().add(existing);

        ReorderResponse result = reorderService.reorder(USER_ID, ORDER_NUMBER);

        verify(cartService).addItemToCart(USER_ID, 5, 1);
        assertEquals(ReorderResponse.LIMITED_STOCK, result.added().get(0).reason());
    }

    @Test
    @DisplayName("skips an item when the cart already holds all the stock")
    void skipsWhenCartAlreadyHoldsAllStock() {
        orderLine(5, 1);
        product(5, "Sleeves", 3, true);
        CartItem existing = new CartItem();
        existing.setProductId(5);
        existing.setQuantity(3);
        cart.getItems().add(existing);

        ReorderResponse result = reorderService.reorder(USER_ID, ORDER_NUMBER);

        verify(cartService, never()).addItemToCart(anyInt(), anyInt(), anyInt());
        assertEquals(1, result.skippedCount());
    }

    @Test
    @DisplayName("handles a mixed order and reports every line")
    void mixedOrder() {
        orderLine(1, 1);
        orderLine(2, 1);
        orderLine(3, 1);
        product(1, "In stock", 9, true);
        when(productRepository.findById(2)).thenReturn(Optional.empty());
        product(3, "Sold out", 0, true);

        ReorderResponse result = reorderService.reorder(USER_ID, ORDER_NUMBER);

        assertEquals(1, result.addedCount());
        assertEquals(2, result.skippedCount());
        verify(cartService).addItemToCart(USER_ID, 1, 1);
    }

    @Test
    @DisplayName("merges duplicate lines of the same product into one request")
    void mergesDuplicateLines() {
        orderLine(1, 1);
        orderLine(1, 2);
        product(1, "Duplicate", 9, true);

        ReorderResponse result = reorderService.reorder(USER_ID, ORDER_NUMBER);

        verify(cartService).addItemToCart(USER_ID, 1, 3);
        assertEquals(1, result.addedCount());
    }

    @Test
    @DisplayName("returns 404 for an order that does not belong to the buyer")
    void unknownOrderIsNotFound() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> reorderService.reorder(USER_ID, "ORD-20260101-9999"));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(cartService, never()).addItemToCart(any(), any(), any());
    }

    @Test
    @DisplayName("returns 400 when the order number is blank")
    void blankOrderNumberIsBadRequest() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> reorderService.reorder(USER_ID, "  "));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }
}
