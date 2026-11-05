package org.example.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.PromotionCodeRequest;
import org.example.dto.PromotionRequest;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PromotionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PromotionRepository promotionRepository;

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

    private User admin;
    private User customer;
    private String adminToken;
    private String customerToken;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        promotionRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        cardGameRepository.deleteAll();
        userRepository.deleteAll();

        admin = new User();
        admin.setUsername("admin1");
        admin.setEmail("admin1@optracard.test");
        admin.setPassword(passwordEncoder.encode("password"));
        admin.setRole("ADMIN");
        admin = userRepository.save(admin);

        customer = new User();
        customer.setUsername("cust1");
        customer.setEmail("cust1@optracard.test");
        customer.setPassword(passwordEncoder.encode("password"));
        customer.setRole("USER");
        customer = userRepository.save(customer);

        adminToken = "Bearer " + jwtUtil.generateToken(admin.getEmail(), admin.getRole());
        customerToken = "Bearer " + jwtUtil.generateToken(customer.getEmail(), customer.getRole());

        CardGame game = new CardGame();
        game.setGameName("Pokemon");
        game = cardGameRepository.save(game);

        testProduct = new Product();
        testProduct.setGameId(game.getGameId());
        testProduct.setProName("Ultra Rare Card");
        testProduct.setProCost(50.0);
        testProduct.setProPriceOfSell(200.0);
        testProduct.setProQuantity(10);
        testProduct.setProType("Single");
        testProduct.setIsActive(true);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("Non-admin user cannot create promotions")
    void customer_createPromotion_forbidden() throws Exception {
        PromotionRequest request = new PromotionRequest(
                "DISCOUNT10", "10% Off", "PERCENTAGE", 10.0, null, 50.0, 100, null,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(10), true
        );

        mockMvc.perform(post("/api/admin/promotions")
                        .header(HttpHeaders.AUTHORIZATION, customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin creates promotion, customer adds product to cart and previews discount successfully")
    void promotionLifecycle_endToEnd() throws Exception {
        // 1. Admin creates promotion
        PromotionRequest promoReq = new PromotionRequest(
                "SAVE20", "Save 20%", "PERCENTAGE", 20.0, null, 50.0, 100, null,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(10), true
        );

        MvcResult createResult = mockMvc.perform(post("/api/admin/promotions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(promoReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SAVE20"))
                .andReturn();

        Integer promoId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        // 2. Admin retrieves promotions list
        mockMvc.perform(get("/api/admin/promotions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("SAVE20"));

        // 3. Customer adds product to cart ($200)
        mockMvc.perform(post("/api/cart/add")
                        .header(HttpHeaders.AUTHORIZATION, customerToken)
                        .param("userId", customer.getUaId().toString())
                        .param("productId", testProduct.getProId().toString())
                        .param("quantity", "1"))
                .andExpect(status().isOk());

        // 4. Customer previews promotion (20% of 200 = 40 discount)
        PromotionCodeRequest codeReq = new PromotionCodeRequest("SAVE20");
        mockMvc.perform(post("/api/orders/promotion-preview")
                        .header(HttpHeaders.AUTHORIZATION, customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(codeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SAVE20"))
                .andExpect(jsonPath("$.discountAmount").value(40.0))
                .andExpect(jsonPath("$.discountedSubtotal").value(160.0))
                .andExpect(jsonPath("$.discountType").value("PERCENTAGE"))
                .andExpect(jsonPath("$.minimumOrderAmount").value(50.0));

        // 5. Admin deletes promotion
        mockMvc.perform(delete("/api/admin/promotions/" + promoId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNoContent());

        // 6. Verify promotion is no longer in list
        mockMvc.perform(get("/api/admin/promotions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}

