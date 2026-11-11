package org.example.integration;

import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userToken;
    private String sellerToken;
    private String adminToken;
    private String superAdminToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User normalUser = new User();
        normalUser.setEmail("user@optracard.test");
        normalUser.setPassword(passwordEncoder.encode("password"));
        normalUser.setUsername("Normal User");
        normalUser.setRole("USER");
        userRepository.save(normalUser);

        User sellerUser = new User();
        sellerUser.setEmail("seller@optracard.test");
        sellerUser.setPassword(passwordEncoder.encode("password"));
        sellerUser.setUsername("Seller User");
        sellerUser.setRole("SELLER");
        userRepository.save(sellerUser);

        User adminUser = new User();
        adminUser.setEmail("admin@optracard.test");
        adminUser.setPassword(passwordEncoder.encode("password"));
        adminUser.setUsername("Admin User");
        adminUser.setRole("ADMIN");
        userRepository.save(adminUser);

        User superAdminUser = new User();
        superAdminUser.setEmail("superadmin@optracard.test");
        superAdminUser.setPassword(passwordEncoder.encode("password"));
        superAdminUser.setUsername("SuperAdmin User");
        superAdminUser.setRole("SUPER_ADMIN");
        userRepository.save(superAdminUser);

        userToken = "Bearer " + jwtUtil.generateToken("user@optracard.test", "USER");
        sellerToken = "Bearer " + jwtUtil.generateToken("seller@optracard.test", "SELLER");
        adminToken = "Bearer " + jwtUtil.generateToken("admin@optracard.test", "ADMIN");
        superAdminToken = "Bearer " + jwtUtil.generateToken("superadmin@optracard.test", "SUPER_ADMIN");
    }

    @Test
    @DisplayName("Public endpoints allow anonymous access")
    void publicEndpoints_anonymousAllowed() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated request to protected admin endpoint is forbidden")
    void anonymous_adminEndpoint_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER role accessing admin endpoint returns 403 Forbidden")
    void userRole_adminEndpoint_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER role accessing seller endpoint returns 403 Forbidden")
    void userRole_sellerEndpoint_forbidden() throws Exception {
        mockMvc.perform(get("/api/seller/products")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN role accessing superadmin endpoint returns 403 Forbidden")
    void adminRole_superadminEndpoint_forbidden() throws Exception {
        mockMvc.perform(get("/api/superadmin/overview")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN role accessing admin dashboard returns 200 OK")
    void adminRole_adminEndpoint_allowed() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SUPER_ADMIN role accessing superadmin overview returns 200 OK")
    void superAdminRole_superadminEndpoint_allowed() throws Exception {
        mockMvc.perform(get("/api/superadmin/overview")
                        .header("Authorization", superAdminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Request with invalid or forged JWT token is rejected")
    void forgedToken_rejected() throws Exception {
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer invalid.forged.jwt.token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Auction mutations deny anonymous and wrong-role callers before service access")
    void auctionMutationRbac_isEnforced() throws Exception {
        mockMvc.perform(post("/api/auctions/1/bids")
                        .contentType("application/json")
                        .content("{\"amount\":1000}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/seller/auctions")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/auctions")
                        .header("Authorization", sellerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/superadmin/settlements")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden());
    }
}
