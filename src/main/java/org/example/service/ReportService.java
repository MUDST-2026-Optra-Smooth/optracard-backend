package org.example.service;

import org.example.dto.WeeklyReportResponse; 
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    @PersistenceContext
    private EntityManager entityManager;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WeeklyReportResponse generateWeeklyReport(LocalDate startDate, LocalDate endDate) {
        try {
            String startStr = startDate.atStartOfDay().format(DATETIME_FORMATTER);
            String endStr = endDate.atTime(23, 59, 59).format(DATETIME_FORMATTER);

            Query query1 = entityManager.createNativeQuery(
                "SELECT COALESCE(SUM(ord_total_price), 0), COUNT(ord_id) " +
                "FROM orders " +
                "WHERE o.ord_createdate BETWEEN CAST(:start AS TIMESTAMP) AND CAST(:end AS TIMESTAMP)"
            )
            .setParameter("start", startStr + " 00:00:00")
            .setParameter("end", endStr + " 23:59:59");

            Object[] orderStats = getSingleRow(query1);
            BigDecimal revenue = parseBigDecimal(orderStats[0]);
            long totalOrders = parseLong(orderStats[1]);
            

            Query query2 = entityManager.createNativeQuery(
                "SELECT COALESCE(SUM(oi.orditems_quantity * COALESCE(p.pro_cost, 0)), 0), COALESCE(SUM(oi.orditems_quantity), 0) " +
                "FROM orderitems oi " +
                "JOIN products p ON oi.pro_id = p.pro_id " +
                "JOIN orders o ON oi.ord_id = o.ord_id " +
                "WHERE o.ord_createdate BETWEEN CAST(:start AS TIMESTAMP) AND CAST(:end AS TIMESTAMP)"
            )
            .setParameter("start", startStr + " 00:00:00")
            .setParameter("end", endStr + " 23:59:59");

            Object[] itemStats = getSingleRow(query2);
            BigDecimal cost = parseBigDecimal(itemStats[0]);
            long itemsSold = parseLong(itemStats[1]);

            BigDecimal profit = revenue.subtract(cost);

            long daysSelected = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            if (daysSelected <= 0) daysSelected = 1; 

            @SuppressWarnings("unchecked")
            List<Object[]> topProductsData = entityManager.createNativeQuery(
                "SELECT p.pro_id, p.pro_name, p.pro_quantity, COALESCE(SUM(oi.orditems_quantity), 0) as qty, COALESCE(SUM(oi.orditems_price * oi.orditems_quantity), 0) as total_rev " +
                "FROM orderitems oi " +
                "JOIN products p ON oi.pro_id = p.pro_id " +
                "JOIN orders o ON oi.ord_id = o.ord_id " +
                "WHERE o.ord_createdate BETWEEN CAST(:start AS TIMESTAMP) AND CAST(:end AS TIMESTAMP)" +
                "GROUP BY p.pro_id, p.pro_name, p.pro_quantity " +
                "ORDER BY qty DESC " +
                "LIMIT 5"
            )
            .setParameter("start", startStr + " 00:00:00")
            .setParameter("end", endStr + " 23:59:59")
            .getResultList();

            List<WeeklyReportResponse.TopProductDto> topProducts = new ArrayList<>();
            if (topProductsData != null) {
                for (Object[] row : topProductsData) {
                    Long pId = parseLong(row[0]);
                    String pName = row[1] != null ? row[1].toString() : "Unknown Product";
                    long pStock = parseLong(row[2]); 
                    long pQty = parseLong(row[3]);
                    BigDecimal pRev = parseBigDecimal(row[4]);
                    
                    Integer daysUntilOut = null;
                    Integer recommendedRestock = null;
                    String status;

                    if (pQty == 0 || daysSelected < 3) {
                        status = "ยังมีข้อมูลไม่เพียงพอ";
                    } else {
                        double avgDailySales = (double) pQty / daysSelected;
                        daysUntilOut = (int) (pStock / avgDailySales);
                        recommendedRestock = (int) Math.ceil(avgDailySales * 14); 

                        if (daysUntilOut <= 3) {
                            status = "ควรเติมด่วน";
                        } else if (daysUntilOut <= 7) {
                            status = "ใกล้หมด";
                        } else {
                            status = "ปกติ";
                        }
                    }
                    
                    topProducts.add(new WeeklyReportResponse.TopProductDto(
                        pId, pName, pQty, pRev, pStock, daysUntilOut, recommendedRestock, status
                    ));
                }
            }

            return new WeeklyReportResponse(revenue, cost, profit, totalOrders, itemsSold, topProducts);

        } catch (Exception e) {
            e.printStackTrace();
            return new WeeklyReportResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, new ArrayList<>()
            );
        }
    }

    private Object[] getSingleRow(Query query) {
        try {
            Object result = query.getSingleResult();
            if (result instanceof Object[]) {
                return (Object[]) result;
            } else if (result != null) {
                return new Object[]{result, 0};
            }
        } catch (Exception e) {
        }
        return new Object[]{0, 0};
    }

    private BigDecimal parseBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private long parseLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (Exception e) {
            return 0L;
        }
    }
}