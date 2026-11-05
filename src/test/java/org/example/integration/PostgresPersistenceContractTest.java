package org.example.integration;

import org.example.model.Promotion;
import org.example.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Guards the PostgreSQL mapping separately from the fast H2 test suite.
 * It is intentionally opt-in so pull requests do not require a Docker daemon.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.datasource.driver-class-name=org.postgresql.Driver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class PostgresPersistenceContractTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("optracard_contract")
            .withUsername("contract")
            .withPassword("contract");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PromotionRepository promotionRepository;

    @Test
    void persistsPromotionRulesAndReadsThemBackWithPostgreSqlTypes() {
        Promotion promotion = new Promotion();
        promotion.setCode("E2E_PG_RULES");
        promotion.setName("PostgreSQL contract rule");
        promotion.setDiscountType("PERCENTAGE");
        promotion.setDiscountValue(50d);
        promotion.setMaximumDiscountAmount(100d);
        promotion.setMinimumOrderAmount(500d);
        promotion.setPerUserLimit(1);
        promotion.setUsageCount(0);
        promotion.setActive(true);
        promotion.setStartsAt(LocalDateTime.now().minusHours(1));
        promotion.setEndsAt(LocalDateTime.now().plusHours(1));

        Promotion saved = promotionRepository.saveAndFlush(promotion);
        Promotion restored = promotionRepository.findById(saved.getPromotionId()).orElseThrow();

        assertNotNull(restored.getPromotionId());
        assertEquals("E2E_PG_RULES", restored.getCode());
        assertEquals(100d, restored.getMaximumDiscountAmount());
        assertEquals(500d, restored.getMinimumOrderAmount());
        assertEquals(1, restored.getPerUserLimit());
    }
}
