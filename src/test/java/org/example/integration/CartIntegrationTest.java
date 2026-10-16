package org.example.integration;

import org.example.model.CardGame;
import org.example.model.Product;
import org.example.model.User;
import org.example.repository.CardGameRepository;
import org.example.repository.CartRepository;
import org.example.repository.ProductRepository;
import org.example.repository.UserRepository;
import org.example.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CardGameRepository cardGameRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private String jwtToken;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
        productRepository.deleteAll();
        cardGameRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("cart_user");
        testUser.setEmail("cart@example.com");
        testUser.setPassword(passwordEncoder.encode("Password123"));
        testUser.setRole("USER");
        testUser = userRepository.save(testUser);

        jwtToken = jwtUtil.generateToken(testUser.getEmail(), testUser.getRole());

        CardGame game = new CardGame();
        game.setGameName("Pokemon");
        game = cardGameRepository.save(game);

        testProduct = new Product();
        testProduct.setGameId(game.getGameId());
        testProduct.setProName("Booster Pack");
        testProduct.setProCost(3.0);
        testProduct.setProPriceOfSell(5.0);
        testProduct.setProQuantity(50);
        testProduct.setProType("Pack");
        testProduct.setIsActive(true);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("Should successfully add item to cart, update quantity, and remove item")
    void fullCartLifecycle_endToEnd() throws Exception {
        // 1. Add item to cart
        mockMvc.perform(post("/api/cart/add")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .param("userId", testUser.getUaId().toString())
                        .param("productId", testProduct.getProId().toString())
                        .param("quantity", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name").value("Booster Pack"))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.subtotal").value(15.0));

        // 2. Fetch cart contents
        mockMvc.perform(get("/api/cart/" + testUser.getUaId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(3));

        // 3. Update quantity
        mockMvc.perform(put("/api/cart/update")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .param("userId", testUser.getUaId().toString())
                        .param("productId", testProduct.getProId().toString())
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.subtotal").value(25.0));

        // 4. Remove item from cart
        mockMvc.perform(delete("/api/cart/remove")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .param("userId", testUser.getUaId().toString())
                        .param("productId", testProduct.getProId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.subtotal").value(0.0));
    }

    @Test
    @DisplayName("User cannot access or modify another user's cart (returns 403 Forbidden)")
    void accessOtherUsersCart_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/cart/99999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Clearing cart removes all items")
    void clearCart_removesAllItems() throws Exception {
        // Add item first
        mockMvc.perform(post("/api/cart/add")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .param("userId", testUser.getUaId().toString())
                        .param("productId", testProduct.getProId().toString())
                        .param("quantity", "2"))
                .andExpect(status().isOk());

        // Clear cart
        mockMvc.perform(delete("/api/cart/clear/" + testUser.getUaId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }
}
