package org.example.service;

import org.example.dto.CheckoutResponse;
import org.example.dto.CreateOrderRequest;
import org.example.model.Cart;
import org.example.model.CartItem;
import org.example.model.MarketplaceStore;
import org.example.model.Order;
import org.example.model.Product;
import org.example.repository.CartRepository;
import org.example.repository.OrderRepository;
import org.example.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    private OrderService orderService;
    private Product officialProduct;
    private Product marketplaceProduct;
    private Cart cart;
    private List<Order> savedOrders;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, cartRepository, productRepository);
        officialProduct = product(10, "Official card", 100d, "OFFICIAL", null);

        MarketplaceStore seller = new MarketplaceStore();
        seller.setStoreId(21);
        seller.setStoreName("Card Corner BKK");
        seller.setStoreStatus("APPROVED");
        marketplaceProduct = product(11, "Marketplace card", 200d, "MARKETPLACE", seller);

        cart = new Cart();
        cart.setUserId(7);
        cart.getItems().add(cartItem(officialProduct.getProId(), 1));
        cart.getItems().add(cartItem(marketplaceProduct.getProId(), 1));

        when(cartRepository.findByUserId(7)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10)).thenReturn(Optional.of(officialProduct));
        when(productRepository.findById(11)).thenReturn(Optional.of(marketplaceProduct));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        savedOrders = new ArrayList<>();
        AtomicInteger nextId = new AtomicInteger(1);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(nextId.getAndIncrement());
            savedOrders.add(order);
            return order;
        });
    }

    @Test
    void mixedOfficialAndMarketplaceCartCreatesOneOrderForEachSeller() {
        CheckoutResponse result = orderService.placeOrder(7,
                new CreateOrderRequest("Kendo", "0812345678", "Bangkok", "standard", "cash"));

        assertEquals(2, result.orderCount());
        assertEquals(400d, result.total());
        assertEquals(2, savedOrders.size());

        Order officialOrder = savedOrders.stream()
                .filter(order -> "OFFICIAL".equals(order.getSource()))
                .findFirst().orElseThrow();
        assertEquals("Optracard Official Store", officialOrder.getStoreName());
        assertEquals(null, officialOrder.getStoreId());
        assertEquals(150d, officialOrder.getTotalPrice());
        assertEquals(10, officialOrder.getItems().get(0).getProductId());

        Order marketplaceOrder = savedOrders.stream()
                .filter(order -> "MARKETPLACE".equals(order.getSource()))
                .findFirst().orElseThrow();
        assertEquals(21, marketplaceOrder.getStoreId());
        assertEquals("Card Corner BKK", marketplaceOrder.getStoreName());
        assertEquals(250d, marketplaceOrder.getTotalPrice());
        assertEquals(11, marketplaceOrder.getItems().get(0).getProductId());

        assertEquals(4, officialProduct.getProQuantity());
        assertEquals(4, marketplaceProduct.getProQuantity());
        assertEquals(0, cart.getItems().size());
        verify(orderRepository, times(2)).saveAndFlush(any(Order.class));
    }

    private Product product(Integer id, String name, Double price, String source, MarketplaceStore store) {
        Product product = new Product();
        product.setProId(id);
        product.setProName(name);
        product.setProPriceOfSell(price);
        product.setProQuantity(5);
        product.setIsActive(true);
        product.setListingSource(source);
        product.setStore(store);
        return product;
    }

    private CartItem cartItem(Integer productId, Integer quantity) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}
