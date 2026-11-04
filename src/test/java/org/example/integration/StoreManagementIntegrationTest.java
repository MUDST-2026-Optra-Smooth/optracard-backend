package org.example.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.controller.AdminStoreController;
import org.example.dto.StoreApplicationRequest;
import org.example.model.User;
import org.example.repository.MarketplaceStoreRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoreManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MarketplaceStoreRepository storeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User applicant;
    private User admin;
    private String applicantToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        storeRepository.deleteAll();
        userRepository.deleteAll();

        applicant = new User();
        applicant.setUsername("seller_applicant");
        applicant.setEmail("applicant@optracard.test");
        applicant.setPassword(passwordEncoder.encode("password"));
        applicant.setRole("USER");
        applicant = userRepository.save(applicant);

        admin = new User();
        admin.setUsername("admin_reviewer");
        admin.setEmail("admin@optracard.test");
        admin.setPassword(passwordEncoder.encode("password"));
        admin.setRole("ADMIN");
        admin = userRepository.save(admin);

        applicantToken = "Bearer " + jwtUtil.generateToken(applicant.getEmail(), applicant.getRole());
        adminToken = "Bearer " + jwtUtil.generateToken(admin.getEmail(), admin.getRole());
    }

    @Test
    @DisplayName("Applicant submits store application, admin reviews and approves it, verifying RBAC enforcement")
    void storeApplicationAndReview_lifecycle() throws Exception {
        // 1. Initially user has no store application
        mockMvc.perform(get("/api/stores/my")
                        .header(HttpHeaders.AUTHORIZATION, applicantToken))
                .andExpect(status().isNoContent());

        // 2. User submits store application
        StoreApplicationRequest application = new StoreApplicationRequest(
                "Card Master Shop", "Best cards in town", true,
                "John", "Doe", "applicant@optracard.test", "0812345678",
                "Kasikorn Bank", "Siam Branch", "John Doe", "123-4-56789-0",
                "123 Sukhumvit Rd", "Bangkok", "Watthana", "Khlong Toei Nuea", "10110",
                "profile.jpg", "passbook.jpg", true
        );

        MvcResult submitResult = mockMvc.perform(post("/api/stores/my")
                        .header(HttpHeaders.AUTHORIZATION, applicantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(application)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeName").value("Card Master Shop"))
                .andExpect(jsonPath("$.storeStatus").value("PENDING"))
                .andReturn();

        Integer storeId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("storeId").asInt();

        // 3. Regular user attempting to review/approve their own store is rejected (403 Forbidden)
        AdminStoreController.ReviewRequest selfReview = new AdminStoreController.ReviewRequest("APPROVED", "Self-approved");
        mockMvc.perform(put("/api/admin/stores/" + storeId + "/review")
                        .header(HttpHeaders.AUTHORIZATION, applicantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfReview)))
                .andExpect(status().isForbidden());

        // 4. Admin reviews and approves store
        AdminStoreController.ReviewRequest adminApproval = new AdminStoreController.ReviewRequest("APPROVED", "Verified identity and bank passbook");
        mockMvc.perform(put("/api/admin/stores/" + storeId + "/review")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminApproval)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeStatus").value("APPROVED"));

        // 5. User checks store application status again -> now APPROVED
        mockMvc.perform(get("/api/stores/my")
                        .header(HttpHeaders.AUTHORIZATION, applicantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeStatus", is("APPROVED")));
    }
}

