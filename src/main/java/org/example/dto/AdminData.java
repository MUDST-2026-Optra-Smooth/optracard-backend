package org.example.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Response and request shapes used only by the administrator workspace. */
public final class AdminData {
    private AdminData() {}

    public record ProductResponse(
            Integer id, String name, String game, String type, Double cost, Double price, Integer stock,
            String imageUrl, String productSet, String language, String description, Boolean active,
            String listingSource, String approvalStatus, Integer storeId, String storeName
    ) {}

    public record ProductRequest(
            String name, String game, String type, Double cost, Double price, Integer stock,
            String imageUrl, String productSet, String language, String description
    ) {}

    public record CardGameResponse(Integer id, String name) {}

    public record OrderItemResponse(
            Integer productId, String name, String game, String imageUrl, Double price, Integer quantity
    ) {}

    public record OrderResponse(
            Integer id, String orderNumber, Integer buyerId, String buyerName, LocalDateTime createdAt,
            Double total, String source, Integer storeId, String storeName, String status, String paymentStatus,
            String shippingMethod, String shippingAddress, String recipientName, String recipientPhone,
            String trackingNumber, List<OrderItemResponse> items
    ) {}

    public record OrderStatusRequest(String status) {}

    public record StoreResponse(
            Integer storeId, String storeSlug, String storeStatus, String storeName, String storeDescription,
            Boolean physicalStore, String ownerName, String ownerEmail, String ownerPhone, String bankName,
            String bankBranch, String bankAccountName, String bankAccountNumber, String storeAddress,
            String province, String district, String subdistrict, String postalCode, String storeProfileImage,
            String bankPassbookImage, Boolean termsAccepted, LocalDateTime submittedAt, LocalDateTime reviewedAt,
            String reviewNote, int productCount
    ) {}

    public record MonthlyMetricResponse(String month, double revenue, double expenses) {}

    public record DashboardResponse(
            double totalRevenue, double totalExpenses, double totalProfit, int totalOrders, int totalItems,
            double averageOrderValue, int activeOfficialProducts, int activeMarketplaceProducts,
            int pendingStoreVerifications, int pendingMarketplaceRequests, int lowStockProducts,
            List<MonthlyMetricResponse> monthlyMetrics, List<OrderResponse> recentOrders
    ) {}
}
