package org.example.integration;

import org.example.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDate;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(ReportService.class)
class WeeklyReportIntegrationTest {
    @Autowired ReportService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void readsPersistedSalesAndIncludesFractionalSecondsOnLastDayOnly() {
        jdbc.update("INSERT INTO products (pro_id, pro_name, pro_cost, pro_quantity, pro_listing_source, pro_approval_status) VALUES (901, 'Boundary card', 40, 10, 'OFFICIAL', 'APPROVED')");
        insertOrder(901, "2026-11-01 00:00:00", 100);
        insertOrder(902, "2026-11-07 23:59:59.999999", 200);
        insertOrder(903, "2026-11-08 00:00:00", 999);
        insertOrder(904, "2026-10-31 23:59:59", 999);
        jdbc.update("INSERT INTO orderitems (ord_id, pro_id, orditems_quantity, orditems_price) VALUES (901, 901, 1, 100), (902, 901, 2, 100), (903, 901, 1, 999)");
        var report = service.generateWeeklyReport(LocalDate.of(2026,11,1), LocalDate.of(2026,11,7));
        assertEquals(0, new BigDecimal("300").compareTo(report.getRevenue()));
        assertEquals(0, new BigDecimal("120").compareTo(report.getCost()));
        assertEquals(2, report.getTotalOrders());
        assertEquals(3, report.getItemsSold());
        assertEquals("Boundary card", report.getTopProducts().get(0).getName());
        assertEquals(3, report.getTopProducts().get(0).getQuantitySold());
    }

    @Test
    void emptyPeriodReturnsSuccessfulZeroTotals() {
        var report = service.generateWeeklyReport(LocalDate.of(2020,1,1), LocalDate.of(2020,1,1));
        assertEquals(0, report.getRevenue().compareTo(BigDecimal.ZERO));
        assertEquals(0, report.getTotalOrders());
        assertTrue(report.getTopProducts().isEmpty());
    }

    private void insertOrder(int id, String date, double total) {
        jdbc.update("INSERT INTO orders (ord_id, ua_id, ord_total_price, ord_source, ord_createdate) VALUES (?, 1, ?, 'OFFICIAL', CAST(? AS TIMESTAMP))", id, total, date);
    }
}
