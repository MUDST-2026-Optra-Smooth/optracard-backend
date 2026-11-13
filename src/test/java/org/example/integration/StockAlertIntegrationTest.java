package org.example.integration;

import jakarta.persistence.EntityManager;
import org.example.model.MarketplaceStore;
import org.example.model.Product;
import org.example.model.User;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.ProductRepository;
import org.example.repository.StockAlertRepository;
import org.example.repository.UserRepository;
import org.example.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:optracard_stock_alert_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StockAlertIntegrationTest {
    private static final String ALERTS = "/api/stock-alerts";
    private static final String SETTINGS = ALERTS + "/settings";

    @Autowired private MockMvc mvc;
    @Autowired private JwtUtil jwt;
    @Autowired private UserRepository users;
    @Autowired private ProductRepository products;
    @Autowired private MarketplaceStoreRepository stores;
    @Autowired private StockAlertRepository alerts;
    @Autowired private EntityManager entityManager;

    @Test
    void savingAdminThresholdReconcilesAllMatchingOfficialInventory() throws Exception {
        String token = bearer(createUser("stock-alert-admin", "ADMIN"));
        MarketplaceStore store = createStore(createUser("stock-alert-seller", "SELLER"));
        List<Integer> ids = IntStream.rangeClosed(1, 10)
                .mapToObj(index -> createProduct("Official card " + index, 100, null, true).getProId())
                .toList();
        int boundary = createProduct("At threshold", 102, null, true).getProId();
        int above = createProduct("Above threshold", 103, null, true).getProId();
        int inactive = createProduct("Inactive card", 100, null, false).getProId();
        int marketplace = createProduct("Seller card", 100, store, true).getProId();

        setThreshold(token, 102);

        assertEquals(ids.size(), alerts.findByProductIdIn(ids).size());
        mvc.perform(get(ALERTS).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$", hasSize(11)))
                .andExpect(jsonPath("$[*].productId", hasItems(ids.toArray(Integer[]::new))))
                .andExpect(jsonPath("$[*].productId", hasItems(boundary)))
                .andExpect(jsonPath("$[*].productId", not(hasItems(above))))
                .andExpect(jsonPath("$[*].productId", not(hasItems(inactive))))
                .andExpect(jsonPath("$[*].productId", not(hasItems(marketplace))))
                .andExpect(jsonPath("$[*].threshold", hasItems(102)));

        setThreshold(token, 99);
        mvc.perform(get(ALERTS).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void sellerThresholdStaysInStoreAndStockTypeChangesReactivateReadAlerts() throws Exception {
        User seller = createUser("stock-alert-owner", "SELLER");
        String token = bearer(seller);
        MarketplaceStore store = createStore(seller);
        MarketplaceStore otherStore = createStore(createUser("stock-alert-neighbor", "SELLER"));
        Product card = createProduct("Own card", 100, store, true);
        createProduct("Other card", 100, otherStore, true);
        createProduct("Official card", 100, null, true);

        setThreshold(token, 102);
        int alertId = alerts.findByProductIdIn(List.of(card.getProId())).get(0).getId();
        mvc.perform(get(ALERTS).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(card.getProId()))
                .andExpect(jsonPath("$[0].stock").value(100))
                .andExpect(jsonPath("$[0].read").value(false));

        mvc.perform(put(ALERTS + "/" + alertId + "/read").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mvc.perform(get(ALERTS).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].read").value(true));

        card.setProQuantity(0);
        products.saveAndFlush(card);
        mvc.perform(get(ALERTS).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(alertId))
                .andExpect(jsonPath("$[0].alertType").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$[0].read").value(false));

        entityManager.flush();
        entityManager.clear();
        mvc.perform(get(ALERTS).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(alertId))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    private void setThreshold(String token, int threshold) throws Exception {
        mvc.perform(put(SETTINGS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaultThreshold\":" + threshold + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultThreshold").value(threshold));
    }

    private String bearer(User user) {
        return "Bearer " + jwt.generateToken(user.getEmail(), user.getRole());
    }

    private User createUser(String name, String role) {
        User user = new User();
        user.setUsername(name);
        user.setEmail(name + "@example.test");
        user.setPassword("unused");
        user.setRole(role);
        return users.save(user);
    }

    private MarketplaceStore createStore(User owner) {
        MarketplaceStore store = new MarketplaceStore();
        store.setSellerUserId(owner.getUaId());
        store.setStoreName(owner.getUsername());
        store.setStoreSlug(owner.getUsername());
        store.setStoreStatus("APPROVED");
        return stores.save(store);
    }

    private Product createProduct(String name, int stock, MarketplaceStore store, boolean active) {
        Product product = new Product();
        product.setProName(name);
        product.setProQuantity(stock);
        product.setStore(store);
        product.setListingSource(store == null ? "OFFICIAL" : "MARKETPLACE");
        product.setIsActive(active);
        return products.save(product);
    }
}
