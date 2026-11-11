package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/** Response and request shapes used only by the administrator workspace. */
@Schema(description = "Admin workspace data transfer object container")
public final class AdminData {
    private AdminData() {}

    @Schema(description = "Admin view of a product listing (official or marketplace)")
    public record ProductResponse(
            @Schema(description = "Product ID", example = "42") Integer id,
            @Schema(description = "Product or card name", example = "Charizard VMAX Shiny") String name,
            @Schema(description = "Card game", example = "Pokemon") String game,
            @Schema(description = "Card type", example = "Single") String type,
            @Schema(description = "Cost basis in THB", example = "300.00") Double cost,
            @Schema(description = "Selling price in THB", example = "550.00") Double price,
            @Schema(description = "Available stock", example = "10") Integer stock,
            @Schema(description = "Product image URL") String imageUrl,
            @Schema(description = "Expansion set", example = "Shining Fates") String productSet,
            @Schema(description = "Card language", example = "English") String language,
            @Schema(description = "Product description") String description,
            @Schema(description = "Whether the product is currently listed", example = "true") Boolean active,
            @Schema(description = "Listing origin (OFFICIAL, MARKETPLACE)", example = "OFFICIAL") String listingSource,
            @Schema(description = "Approval status (PENDING, APPROVED, REJECTED)", example = "APPROVED") String approvalStatus,
            @Schema(description = "Owning store ID (null for official)", example = "3") Integer storeId,
            @Schema(description = "Owning store name", example = "Kanto Card Shop") String storeName
    ) {}

    @Schema(description = "Admin product creation or update payload")
    public record ProductRequest(
            @Schema(description = "Product name", example = "Blue-Eyes White Dragon") String name,
            @Schema(description = "Card game", example = "Yu-Gi-Oh!") String game,
            @Schema(description = "Card type", example = "Single") String type,
            @Schema(description = "Cost basis in THB", example = "800.00") Double cost,
            @Schema(description = "Selling price in THB", example = "1500.00") Double price,
            @Schema(description = "Available stock", example = "5") Integer stock,
            @Schema(description = "Product image URL") String imageUrl,
            @Schema(description = "Expansion set", example = "Legend of Blue-Eyes") String productSet,
            @Schema(description = "Card language", example = "English") String language,
            @Schema(description = "Product description") String description
    ) {}

    @Schema(description = "Toggle product active/inactive status")
    public record ActiveRequest(
            @Schema(description = "Desired active state", example = "true") Boolean active
    ) {}

    @Schema(description = "Card game reference")
    public record CardGameResponse(
            @Schema(description = "Game ID", example = "1") Integer id,
            @Schema(description = "Game title", example = "Pokemon") String name
    ) {}

    @Schema(description = "Admin view of an ordered line item")
    public record OrderItemResponse(
            @Schema(description = "Product ID", example = "42") Integer productId,
            @Schema(description = "Product name", example = "Charizard VMAX Shiny") String name,
            @Schema(description = "Card game", example = "Pokemon") String game,
            @Schema(description = "Product image URL") String imageUrl,
            @Schema(description = "Purchased unit price in THB", example = "550.00") Double price,
            @Schema(description = "Quantity purchased", example = "2") Integer quantity
    ) {}

    @Schema(description = "Admin view of a customer order with full details")
    public record OrderResponse(
            @Schema(description = "Order record ID", example = "201") Integer id,
            @Schema(description = "Order number", example = "ORD-20261111-9871") String orderNumber,
            @Schema(description = "Buyer user ID", example = "101") Integer buyerId,
            @Schema(description = "Buyer display name", example = "kaiba_corp") String buyerName,
            @Schema(description = "Order creation timestamp") LocalDateTime createdAt,
            @Schema(description = "Grand total in THB", example = "1050.00") Double total,
            @Schema(description = "Inventory origin (OFFICIAL, MARKETPLACE)", example = "OFFICIAL") String source,
            @Schema(description = "Fulfilling store ID", example = "3") Integer storeId,
            @Schema(description = "Fulfilling store name", example = "Kanto Card Shop") String storeName,
            @Schema(description = "Order status", example = "PROCESSING") String status,
            @Schema(description = "Payment status", example = "PAID") String paymentStatus,
            @Schema(description = "Payment method", example = "PROMPTPAY") String paymentMethod,
            @Schema(description = "Shipping method", example = "STANDARD") String shippingMethod,
            @Schema(description = "Delivery address") String shippingAddress,
            @Schema(description = "Recipient name", example = "Seto Kaiba") String recipientName,
            @Schema(description = "Recipient phone", example = "+66812345678") String recipientPhone,
            @Schema(description = "Package tracking number", example = "TH1234567890") String trackingNumber,
            @Schema(description = "Ordered items") List<OrderItemResponse> items,
            @Schema(description = "Shipping fee in THB", example = "50.00") Double shippingFee,
            @Schema(description = "Items subtotal before discount", example = "1100.00") Double subtotal,
            @Schema(description = "Applied coupon code", example = "SUMMER2026") String discountCode,
            @Schema(description = "Applied coupon name", example = "Summer TCG Clearance") String discountName,
            @Schema(description = "Discount amount in THB", example = "100.00") Double discountAmount
    ) {
        public OrderResponse(Integer id, String orderNumber, Integer buyerId, String buyerName, LocalDateTime createdAt,
                             Double total, String source, Integer storeId, String storeName, String status,
                             String paymentStatus, String paymentMethod, String shippingMethod, String shippingAddress,
                             String recipientName, String recipientPhone, String trackingNumber,
                             List<OrderItemResponse> items) {
            this(id, orderNumber, buyerId, buyerName, createdAt, total, source, storeId, storeName, status,
                    paymentStatus, paymentMethod, shippingMethod, shippingAddress, recipientName, recipientPhone,
                    trackingNumber, items, null, null, null, null, 0d);
        }
    }

    @Schema(description = "Admin order status update payload")
    public record OrderStatusRequest(
            @Schema(description = "New order status (PROCESSING, SHIPPED, DELIVERED, CANCELLED)", example = "SHIPPED") String status,
            @Schema(description = "Carrier package tracking number", example = "TH1234567890") String trackingNumber
    ) {}

    @Schema(description = "Admin view of a merchant store application")
    public record StoreResponse(
            @Schema(description = "Store ID", example = "5") Integer storeId,
            @Schema(description = "Store URL slug", example = "siam-card-arena") String storeSlug,
            @Schema(description = "Application status (PENDING, APPROVED, REJECTED)", example = "APPROVED") String storeStatus,
            @Schema(description = "Store display name", example = "Siam Card Arena") String storeName,
            @Schema(description = "Store description") String storeDescription,
            @Schema(description = "Physical storefront indicator", example = "true") Boolean physicalStore,
            @Schema(description = "Owner full name", example = "Somchai Prasert") String ownerName,
            @Schema(description = "Owner email") String ownerEmail,
            @Schema(description = "Owner phone") String ownerPhone,
            @Schema(description = "Bank name") String bankName,
            @Schema(description = "Bank branch") String bankBranch,
            @Schema(description = "Account holder name") String bankAccountName,
            @Schema(description = "Bank account number") String bankAccountNumber,
            @Schema(description = "Store address") String storeAddress,
            @Schema(description = "Province") String province,
            @Schema(description = "District") String district,
            @Schema(description = "Subdistrict") String subdistrict,
            @Schema(description = "Postal code", example = "10330") String postalCode,
            @Schema(description = "Store profile image URL") String storeProfileImage,
            @Schema(description = "Bank passbook document URL") String bankPassbookImage,
            @Schema(description = "Terms acceptance flag", example = "true") Boolean termsAccepted,
            @Schema(description = "Submission timestamp") LocalDateTime submittedAt,
            @Schema(description = "Review timestamp") LocalDateTime reviewedAt,
            @Schema(description = "Review notes or rejection feedback") String reviewNote,
            @Schema(description = "Total product listings in store", example = "12") int productCount
    ) {}

    @Schema(description = "Monthly revenue and expense metric")
    public record MonthlyMetricResponse(
            @Schema(description = "Month label (YYYY-MM)", example = "2026-11") String month,
            @Schema(description = "Monthly revenue in THB", example = "45000.00") double revenue,
            @Schema(description = "Monthly expenses in THB", example = "28000.00") double expenses
    ) {}

    @Schema(description = "Admin dashboard KPI summary with financial metrics and recent activity")
    public record DashboardResponse(
            @Schema(description = "Total platform revenue in THB", example = "250000.00") double totalRevenue,
            @Schema(description = "Total platform expenses in THB", example = "150000.00") double totalExpenses,
            @Schema(description = "Net profit in THB", example = "100000.00") double totalProfit,
            @Schema(description = "Total orders placed", example = "328") int totalOrders,
            @Schema(description = "Total items sold", example = "712") int totalItems,
            @Schema(description = "Average order value in THB", example = "762.19") double averageOrderValue,
            @Schema(description = "Active official product count", example = "45") int activeOfficialProducts,
            @Schema(description = "Active marketplace product count", example = "120") int activeMarketplaceProducts,
            @Schema(description = "Pending store verification requests", example = "3") int pendingStoreVerifications,
            @Schema(description = "Pending marketplace listing requests", example = "7") int pendingMarketplaceRequests,
            @Schema(description = "Products with inventory below threshold", example = "5") int lowStockProducts,
            @Schema(description = "12-month revenue and expense trend") List<MonthlyMetricResponse> monthlyMetrics,
            @Schema(description = "Recent orders across all channels") List<OrderResponse> recentOrders
    ) {}

    @Schema(description = "Top-selling product by quantity sold")
    public record TopProductResponse(
            @Schema(description = "Product ID", example = "42") Integer productId,
            @Schema(description = "Product name", example = "Charizard VMAX Shiny") String name,
            @Schema(description = "Total quantity sold", example = "28") Integer soldQuantity
    ) {}

    @Schema(description = "Admin weekly financial and inventory report")
    public record WeeklyReportResponse(
            @Schema(description = "7-day total revenue in THB", example = "18500.00") double totalRevenue,
            @Schema(description = "7-day total cost in THB", example = "11200.00") double totalCost,
            @Schema(description = "7-day net profit in THB", example = "7300.00") double totalProfit,
            @Schema(description = "Number of orders in period", example = "42") int orderCount,
            @Schema(description = "Number of items sold", example = "87") int itemsSold,
            @Schema(description = "Products below low-stock threshold", example = "3") int lowStockCount,
            @Schema(description = "Products with zero inventory", example = "1") int outOfStockCount,
            @Schema(description = "Best-selling products this week") List<TopProductResponse> topProducts
    ) {}

    @Schema(description = "Inventory restocking prediction and recommendation")
    public record RestockPredictionResponse(
            @Schema(description = "Product ID", example = "42") Integer productId,
            @Schema(description = "Product name", example = "Charizard VMAX Shiny") String name,
            @Schema(description = "Current available stock", example = "3") Integer currentStock,
            @Schema(description = "Average daily sales velocity", example = "1.5") Double avgDailySales,
            @Schema(description = "Estimated days until stockout", example = "2") Integer daysUntilOut,
            @Schema(description = "Suggested reorder quantity", example = "15") Integer recommendedRestock,
            @Schema(description = "Urgency status message", example = "LOW — restock within 2 days") String statusMessage
    ) {}
}
