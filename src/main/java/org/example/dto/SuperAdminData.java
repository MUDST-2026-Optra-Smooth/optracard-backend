package org.example.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Response and request shapes for the super-administrator workspace. */
public final class SuperAdminData {
    private SuperAdminData() {}

    public record MonthlyMetricResponse(String month, double revenue, int orders) {}

    public record GameCountResponse(String game, int listings) {}

    public record StorePerformanceResponse(
            Integer storeId, String storeName, String ownerName, String status,
            int productCount, int orderCount, double gmv
    ) {}

    public record OverviewResponse(
            int totalStores, int approvedStores, int pendingStores, int listedProducts, int activeListings,
            double gmvLast30Days, int ordersLast30Days, double averageOrderValueLast30Days,
            int inStockListings, int lowStockListings, int outOfStockListings,
            List<MonthlyMetricResponse> monthlyMetrics,
            List<StorePerformanceResponse> topStores,
            List<GameCountResponse> catalogByGame
    ) {}

    public record CatalogProductResponse(
            Integer id, String name, String game, String type, String productSet, String language,
            Double cost, Double price, Integer stock, String imageUrl, String description,
            Boolean active, String listingSource, String approvalStatus, Integer storeId, String storeName
    ) {}

    public record StoreResponse(
            Integer storeId, String storeName, String storeSlug, String storeStatus, String description,
            String ownerName, String ownerEmail, String ownerPhone, String location,
            LocalDateTime submittedAt, LocalDateTime reviewedAt, int productCount, int activeProductCount,
            int inStockProductCount, int totalStock, int orderCount, double gmv,
            List<CatalogProductResponse> products
    ) {}

    public record TransactionResponse(
            Integer id, String orderNumber, LocalDateTime createdAt, Integer buyerId, String buyerName,
            Integer storeId, String storeName, String source, String status, String paymentStatus,
            String shippingMethod, Double total, int itemCount
    ) {}

    public record UserResponse(
            Integer id, String username, String email, String role, String phone, String address,
            LocalDateTime createdAt, Integer storeId, String storeName, String storeStatus
    ) {}

    public record StaffResponse(
            Integer id, String username, String email, String role, String phone, String address,
            LocalDateTime createdAt
    ) {}

    /** Super admins create standard ADMIN accounts; super-admin elevation remains a database-only action. */
    public record CreateStaffRequest(String username, String email, String password, String phone, String address) {}

    /** Required by the API in addition to the two-step confirmation presented in the workspace. */
    public record DeleteAccountRequest(String confirmation) {}
}
