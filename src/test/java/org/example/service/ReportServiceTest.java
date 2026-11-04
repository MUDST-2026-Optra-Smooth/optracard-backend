package org.example.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.example.dto.WeeklyReportResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("generateWeeklyReport parses orders, costs, and top products correctly")
    void generateWeeklyReport_success() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 7);

        Query query1 = mock(Query.class);
        Query query2 = mock(Query.class);
        Query query3 = mock(Query.class);

        when(entityManager.createNativeQuery(contains("FROM orders"))).thenReturn(query1);
        when(entityManager.createNativeQuery(contains("COALESCE(SUM(oi.orditems_quantity * COALESCE(p.pro_cost"))).thenReturn(query2);
        when(entityManager.createNativeQuery(contains("GROUP BY p.pro_id"))).thenReturn(query3);

        when(query1.setParameter(anyString(), any())).thenReturn(query1);
        when(query2.setParameter(anyString(), any())).thenReturn(query2);
        when(query3.setParameter(anyString(), any())).thenReturn(query3);

        when(query1.getSingleResult()).thenReturn(new Object[]{10000.0, 5L});
        when(query2.getSingleResult()).thenReturn(new Object[]{4000.0, 15L});

        Object[] topProduct1 = new Object[]{1L, "Charizard VMAX", 10L, 5L, 2500.0};
        Object[] topProduct2 = new Object[]{2L, "Pikachu Illustrator", 2L, 4L, 2000.0};
        when(query3.getResultList()).thenReturn(List.of(topProduct1, topProduct2));

        WeeklyReportResponse response = reportService.generateWeeklyReport(start, end);

        assertNotNull(response);
        assertEquals(new BigDecimal("10000.0"), response.getRevenue());
        assertEquals(new BigDecimal("4000.0"), response.getCost());
        assertEquals(new BigDecimal("6000.0"), response.getProfit());
        assertEquals(5L, response.getTotalOrders());
        assertEquals(15L, response.getItemsSold());

        List<WeeklyReportResponse.TopProductDto> topProducts = response.getTopProducts();
        assertEquals(2, topProducts.size());
        assertEquals("Charizard VMAX", topProducts.get(0).getName());
        assertEquals(1L, topProducts.get(0).getProductId());
        assertNotNull(topProducts.get(0).getStatus());
    }

    @Test
    @DisplayName("generateWeeklyReport falls back to zero values when an exception is thrown")
    void generateWeeklyReport_exceptionFallback() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 7);

        when(entityManager.createNativeQuery(anyString())).thenThrow(new RuntimeException("DB offline"));

        WeeklyReportResponse response = reportService.generateWeeklyReport(start, end);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getRevenue());
        assertEquals(BigDecimal.ZERO, response.getCost());
        assertEquals(BigDecimal.ZERO, response.getProfit());
        assertEquals(0L, response.getTotalOrders());
        assertEquals(0L, response.getItemsSold());
        assertTrue(response.getTopProducts().isEmpty());
    }
}

