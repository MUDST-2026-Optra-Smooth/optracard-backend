package org.example.dto;

import java.math.BigDecimal;
import java.util.List;

public class WeeklyReportResponse {
    private BigDecimal revenue;
    private BigDecimal cost;
    private BigDecimal profit;
    private long totalOrders;
    private long itemsSold;
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

    public static class TopProductDto {
        private Long productId;          
        private String name;
        private Long quantitySold;
        private BigDecimal totalRevenue;
        private Long currentStock;
        private Integer daysUntilOut;
        private Integer recommendedRestock;
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