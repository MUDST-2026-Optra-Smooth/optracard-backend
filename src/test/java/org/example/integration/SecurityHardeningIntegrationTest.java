package org.example.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.CardGame;
import org.example.model.Product;
import org.example.model.User;
import org.example.repository.CardGameRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHardeningIntegrationTest {

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
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        cardGameRepository.deleteAll();
        userRepository.deleteAll();

        CardGame game = new CardGame();
        game.setGameName("Pokemon");
        game.setGameDescription("Pokemon TCG");
        game = cardGameRepository.save(game);

        Product p = new Product();
        p.setGameId(game.getGameId());
        p.setProName("Pikachu Illustrator");
        p.setProCost(100.0);
        p.setProPriceOfSell(250.0);
        p.setProQuantity(10);
        p.setProType("Single");
        p.setIsActive(true);
        p.setListingSource("OFFICIAL");
        p.setApprovalStatus("APPROVED");
        productRepository.save(p);
    }

    @Test
    @DisplayName("SQL Injection payloads in search query are handled safely without 500 error or syntax crashes")
    void searchEndpoint_sqlInjectionPayloads_handledSafely() throws Exception {
        String[] sqlPayloads = {
                "' OR '1'='1",
                "'; DROP TABLE users; --",
                "\" OR 1=1 --",
                "' UNION SELECT * FROM users --",
                "admin'--"
        };

        for (String payload : sqlPayloads) {
            mockMvc.perform(get("/api/products/search")
                            .param("q", payload))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/products/catalog")
                            .param("q", payload))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("XSS payloads in catalog query parameters are handled without reflection or server errors")
    void searchEndpoint_xssPayloads_handledSafely() throws Exception {
        String[] xssPayloads = {
                "<script>alert('XSS')</script>",
                "\"><img src=x onerror=alert(1)>",
                "javascript:alert(1)"
        };

        for (String payload : xssPayloads) {
            mockMvc.perform(get("/api/products/search")
                            .param("q", payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", empty()));

            mockMvc.perform(get("/api/products/catalog")
                            .param("q", payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items", empty()));
        }
    }

    @Test
    @DisplayName("Registration enforces default USER role and prevents privilege escalation via request body")
    void registration_preventsPrivilegeEscalation() throws Exception {
        // RegisterRequest only binds username, email, phone, address, password
        // Even if a malicious actor sends JSON containing "role": "SUPER_ADMIN"
        String maliciousJson = """
                {
                    "username": "attacker",
                    "email": "attacker@evil.local",
                    "phone": "0800000000",
                    "address": "Attacker Address",
                    "password": "Password123!",
                    "role": "SUPER_ADMIN"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousJson))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail("attacker@evil.local").orElse(null);
        assertNotNull(user);
        assertEquals("USER", user.getRole(), "User role must be USER, preventing privilege escalation");
    }

    @Test
    @DisplayName("Forged JWT with algorithm 'none' is rejected with 401 or 403")
    void forgedJwt_algNone_rejected() throws Exception {
        // Unsigned JWT with alg none: {"alg":"none"}.{"sub":"admin@optracard.com","role":"ADMIN"}
        String forgedToken = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiJhZG1pbkBvcHRyYWNhcmQuY29tIiwicm9sZSI6IkFETUlOIn0.";

        mockMvc.perform(get("/api/admin/metrics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tampered JWT signature is rejected with 401 or 403")
    void tamperedJwt_signatureModified_rejected() throws Exception {
        String validToken = jwtUtil.generateToken("admin@optracard.com", "ADMIN");
        // Tamper the signature by altering the last characters
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "abcde";

        mockMvc.perform(get("/api/admin/metrics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Malformed Authorization headers are treated as unauthenticated")
    void malformedAuthHeader_treatedAsUnauthenticated() throws Exception {
        String[] badHeaders = {
                "Bearer",
                "Bearer ",
                "Basic YWRtaW46cGFzc3dvcmQ=",
                "Token some-random-hex",
                "Bearer invalid.parts"
        };

        for (String badHeader : badHeaders) {
            mockMvc.perform(get("/api/cart")
                            .header(HttpHeaders.AUTHORIZATION, badHeader))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Invalid or negative product ID lookups return 404 rather than 500 error")
    void negativeOrInvalidId_returns404Safely() throws Exception {
        mockMvc.perform(get("/api/products/-1"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/products/0"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound());
    }
}
