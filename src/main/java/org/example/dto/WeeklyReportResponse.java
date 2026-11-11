package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Legacy weekly financial report with top products and restocking insights")
public class WeeklyReportResponse {
    @Schema(description = "7-day total revenue in THB", example = "18500.00")
    private BigDecimal revenue;

    @Schema(description = "7-day total cost in THB", example = "11200.00")
    private BigDecimal cost;

    @Schema(description = "7-day net profit in THB", example = "7300.00")
    private BigDecimal profit;

    @Schema(description = "Number of orders in period", example = "42")
    private long totalOrders;

    @Schema(description = "Number of individual items sold", example = "87")
    private long itemsSold;

    @Schema(description = "Top-selling products with restocking predictions")
    private List<TopProductDto> topProducts;

    public WeeklyReportResponse(BigDecimal revenue, BigDecimal cost, BigDecimal profit, long totalOrders, long itemsSold, List<TopProductDto> topProducts) {
        this.revenue = revenue;
        this.cost = cost;
        this.profit = profit;
        this.totalOrders = totalOrders;
        this.itemsSold = itemsSold;
        this.topProducts = topProducts;
    }

    public BigDecimal getRevenue() { return revenue; }
    public BigDecimal getCost() { return cost; }
    public BigDecimal getProfit() { return profit; }
    public long getTotalOrders() { return totalOrders; }
    public long getItemsSold() { return itemsSold; }
    public List<TopProductDto> getTopProducts() { return topProducts; }

    @Schema(description = "Top product sales and restocking prediction")
    public static class TopProductDto {
        @Schema(description = "Product ID", example = "42")
        private Long productId;

        @Schema(description = "Product name", example = "Charizard VMAX Shiny")
        private String name;

        @Schema(description = "Total quantity sold", example = "28")
        private Long quantitySold;

        @Schema(description = "Total revenue from this product in THB", example = "15400.00")
        private BigDecimal totalRevenue;

        @Schema(description = "Current available stock", example = "5")
        private Long currentStock;

        @Schema(description = "Estimated days until stockout", example = "3")
        private Integer daysUntilOut;

        @Schema(description = "Suggested reorder quantity", example = "20")
        private Integer recommendedRestock;

        @Schema(description = "Urgency status message", example = "LOW — restock within 3 days")
        private String status;

        public TopProductDto(Long productId, String name, Long quantitySold, BigDecimal totalRevenue,
                             Long currentStock, Integer daysUntilOut, Integer recommendedRestock, String status) {
            this.productId = productId;
            this.name = name;
            this.quantitySold = quantitySold;
            this.totalRevenue = totalRevenue;
            this.currentStock = currentStock;
            this.daysUntilOut = daysUntilOut;
            this.recommendedRestock = recommendedRestock;
            this.status = status;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Long getQuantitySold() { return quantitySold; }
        public void setQuantitySold(Long quantitySold) { this.quantitySold = quantitySold; }

        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

        public Long getCurrentStock() { return currentStock; }
        public void setCurrentStock(Long currentStock) { this.currentStock = currentStock; }

        public Integer getDaysUntilOut() { return daysUntilOut; }
        public void setDaysUntilOut(Integer daysUntilOut) { this.daysUntilOut = daysUntilOut; }

        public Integer getRecommendedRestock() { return recommendedRestock; }
        public void setRecommendedRestock(Integer recommendedRestock) { this.recommendedRestock = recommendedRestock; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
