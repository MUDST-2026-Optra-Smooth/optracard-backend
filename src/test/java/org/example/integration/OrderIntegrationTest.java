package org.example.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.CreateOrderRequest;
import org.example.model.CardGame;
import org.example.model.Product;
import org.example.model.User;
import org.example.repository.*;
import org.example.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CardGameRepository cardGameRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User buyer;
    private User otherUser;
    private String buyerToken;
    private String otherUserToken;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        cardGameRepository.deleteAll();
        userRepository.deleteAll();

        buyer = new User();
        buyer.setUsername("buyer1");
        buyer.setEmail("buyer1@optracard.test");
        buyer.setPassword(passwordEncoder.encode("password123"));
        buyer.setRole("USER");
        buyer = userRepository.save(buyer);

        otherUser = new User();
        otherUser.setUsername("buyer2");
        otherUser.setEmail("buyer2@optracard.test");
        otherUser.setPassword(passwordEncoder.encode("password123"));
        otherUser.setRole("USER");
        otherUser = userRepository.save(otherUser);

        buyerToken = "Bearer " + jwtUtil.generateToken(buyer.getEmail(), buyer.getRole());
        otherUserToken = "Bearer " + jwtUtil.generateToken(otherUser.getEmail(), otherUser.getRole());

        CardGame game = new CardGame();
        game.setGameName("One Piece");
        game = cardGameRepository.save(game);

        testProduct = new Product();
        testProduct.setGameId(game.getGameId());
        testProduct.setProName("OP-05 Booster");
        testProduct.setProCost(50.0);
        testProduct.setProPriceOfSell(100.0);
        testProduct.setProQuantity(30);
        testProduct.setProType("Pack");
        testProduct.setIsActive(true);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("Placing order with empty cart is rejected")
    void placeOrder_emptyCart_rejected() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "Somchai", "0812345678", "Bangkok, Thailand", "standard", "qr"
        );

        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Placing order decrements inventory stock, creates order, and clears user cart")
    void placeOrder_success_lifecycle() throws Exception {
        // 1. Add 4 items to cart
        mockMvc.perform(post("/api/cart/add")
                        .header(HttpHeaders.AUTHORIZATION, buyerToken)
                        .param("userId", buyer.getUaId().toString())
                        .param("productId", testProduct.getProId().toString())
                        .param("quantity", "4"))
                .andExpect(status().isOk());

        // 2. Place order
        CreateOrderRequest request = new CreateOrderRequest(
                "Somchai", "0812345678", "Bangkok, Thailand", "standard", "qr"
        );

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].orderNumber").isNotEmpty())
                .andReturn();

        // 3. Verify stock was decremented from 30 down to 26
        Product updatedProduct = productRepository.findById(testProduct.getProId()).orElseThrow();
        assertEquals(26, updatedProduct.getProQuantity());

        // 4. Verify cart is now empty
        mockMvc.perform(get("/api/cart/" + buyer.getUaId())
                        .header(HttpHeaders.AUTHORIZATION, buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        // 5. Verify order appears in buyer's order history
        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("User cannot access another user's order details")
    void orderDetail_otherUser_unauthorized() throws Exception {
        // 1. Add item and place order as buyer
        mockMvc.perform(post("/api/cart/add")
                        .header(HttpHeaders.AUTHORIZATION, buyerToken)
                        .param("userId", buyer.getUaId().toString())
                        .param("productId", testProduct.getProId().toString())
                        .param("quantity", "2"))
                .andExpect(status().isOk());

        CreateOrderRequest request = new CreateOrderRequest(
                "Somchai", "0812345678", "Bangkok, Thailand", "standard", "qr"
        );

        String json = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(json).get("orders").get(0).get("orderNumber").asText();

        // 2. Buyer can view detail
        mockMvc.perform(get("/api/orders/" + orderNumber)
                        .header(HttpHeaders.AUTHORIZATION, buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber));

        // 3. Other user attempting to view buyer's order is rejected
        mockMvc.perform(get("/api/orders/" + orderNumber)
                        .header(HttpHeaders.AUTHORIZATION, otherUserToken))
                .andExpect(status().is4xxClientError());
    }
}

