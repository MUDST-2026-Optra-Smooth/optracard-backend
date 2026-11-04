package org.example.service;

import jakarta.persistence.criteria.*;
import org.example.model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogProductSpecificationTest {

    @Mock
    private Root<Product> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Join<Object, Object> cardGameJoin;

    @Mock
    private Join<Object, Object> storeJoin;

    @Mock
    private Predicate conjunction;

    @Mock
    private Path<Object> path;

    @Mock
    private Expression<String> stringExpr;

    @Test
    @DisplayName("publicProduct creates specification with null filters")
    void publicProduct_nullFilters() {
        when(root.join(eq("cardGame"), eq(JoinType.LEFT))).thenReturn(cardGameJoin);
        when(root.join(eq("store"), eq(JoinType.LEFT))).thenReturn(storeJoin);
        when(cb.conjunction()).thenReturn(conjunction);
        when(cb.and(any(), any())).thenReturn(conjunction);
        when(cb.or(any(), any(), any())).thenReturn(conjunction);
        when(cb.isTrue(any())).thenReturn(conjunction);
        when(root.get(anyString())).thenReturn(path);

        Specification<Product> spec = CatalogProductSpecification.publicProduct(
                null, null, null, null, null, null, null, null
        );

        Predicate predicate = spec.toPredicate(root, query, cb);
        assertNotNull(predicate);
    }

    @Test
    @DisplayName("publicProduct handles query keyword, numeric game id, price range, and official source")
    void publicProduct_withFilters() {
        when(root.join(eq("cardGame"), eq(JoinType.LEFT))).thenReturn(cardGameJoin);
        when(root.join(eq("store"), eq(JoinType.LEFT))).thenReturn(storeJoin);
        when(cb.conjunction()).thenReturn(conjunction);
        when(cb.and(any(), any())).thenReturn(conjunction);
        when(cb.or(any(), any(), any())).thenReturn(conjunction);
        when(cb.or(any(Predicate[].class))).thenReturn(conjunction);
        when(cb.or(any(), any())).thenReturn(conjunction);
        when(cb.isTrue(any())).thenReturn(conjunction);
        when(cb.equal(any(), any())).thenReturn(conjunction);
        when(cb.greaterThanOrEqualTo(any(), anyDouble())).thenReturn(conjunction);
        when(cb.lessThanOrEqualTo(any(), anyDouble())).thenReturn(conjunction);
        when(cb.like(any(), anyString())).thenReturn(conjunction);
        when(cb.lower(any())).thenReturn(stringExpr);
        when(root.get(anyString())).thenReturn(path);
        when(cardGameJoin.get(anyString())).thenReturn(path);
        when(storeJoin.get(anyString())).thenReturn(path);

        Specification<Product> spec = CatalogProductSpecification.publicProduct(
                "Charizard", "Single", "1", 10.0, 500.0, "EN", "OFFICIAL", 2
        );

        Predicate predicate = spec.toPredicate(root, query, cb);
        assertNotNull(predicate);
    }

    @Test
    @DisplayName("publicProduct handles string game name and marketplace source")
    void publicProduct_stringGameAndMarketplace() {
        when(root.join(eq("cardGame"), eq(JoinType.LEFT))).thenReturn(cardGameJoin);
        when(root.join(eq("store"), eq(JoinType.LEFT))).thenReturn(storeJoin);
        when(cb.conjunction()).thenReturn(conjunction);
        when(cb.and(any(), any())).thenReturn(conjunction);
        when(cb.or(any(), any(), any())).thenReturn(conjunction);
        when(cb.or(any(Predicate[].class))).thenReturn(conjunction);
        when(cb.isTrue(any())).thenReturn(conjunction);
        when(cb.equal(any(), any())).thenReturn(conjunction);
        when(cb.lower(any())).thenReturn(stringExpr);
        when(root.get(anyString())).thenReturn(path);
        when(cardGameJoin.get(anyString())).thenReturn(path);

        Specification<Product> spec = CatalogProductSpecification.publicProduct(
                "", "", "Pokemon", null, null, null, "MARKETPLACE", null
        );

        Predicate predicate = spec.toPredicate(root, query, cb);
        assertNotNull(predicate);
    }
}
