package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/** Response and request shapes for the super-administrator workspace. */
@Schema(description = "Super Admin workspace data transfer object container")
public final class SuperAdminData {
    private SuperAdminData() {}

    @Schema(description = "Monthly revenue and order count metric")
    public record MonthlyMetricResponse(
            @Schema(description = "Month label (YYYY-MM)", example = "2026-11") String month,
            @Schema(description = "Monthly revenue in THB", example = "95000.00") double revenue,
            @Schema(description = "Number of orders in month", example = "142") int orders
    ) {}

    @Schema(description = "Card game catalog distribution")
    public record GameCountResponse(
            @Schema(description = "Game title", example = "Pokemon") String game,
            @Schema(description = "Total product listings for this game", example = "45") int listings
    ) {}

    @Schema(description = "Merchant store performance summary")
    public record StorePerformanceResponse(
            @Schema(description = "Store ID", example = "3") Integer storeId,
            @Schema(description = "Store name", example = "Kanto Card Shop") String storeName,
            @Schema(description = "Store owner name", example = "Somchai Prasert") String ownerName,
            @Schema(description = "Store verification status (PENDING, APPROVED, REJECTED)", example = "APPROVED") String status,
            @Schema(description = "Total product listings", example = "18") int productCount,
            @Schema(description = "Total customer orders fulfilled", example = "52") int orderCount,
            @Schema(description = "Gross Merchandise Value in THB", example = "125000.00") double gmv
    ) {}

    @Schema(description = "Executive platform overview with KPIs, stock health, and top-performing stores")
    public record OverviewResponse(
            @Schema(description = "Total registered stores", example = "15") int totalStores,
            @Schema(description = "Approved active stores", example = "12") int approvedStores,
            @Schema(description = "Pending verification stores", example = "3") int pendingStores,
            @Schema(description = "Total product listings", example = "320") int listedProducts,
            @Schema(description = "Active (in-stock) listings", example = "280") int activeListings,
            @Schema(description = "30-day GMV in THB", example = "450000.00") double gmvLast30Days,
            @Schema(description = "30-day order count", example = "523") int ordersLast30Days,
            @Schema(description = "30-day average order value in THB", example = "860.42") double averageOrderValueLast30Days,
            @Schema(description = "In-stock listing count", example = "250") int inStockListings,
            @Schema(description = "Low-stock listing count", example = "20") int lowStockListings,
            @Schema(description = "Out-of-stock listing count", example = "10") int outOfStockListings,
            @Schema(description = "Monthly revenue and order trend") List<MonthlyMetricResponse> monthlyMetrics,
            @Schema(description = "Top performing stores by GMV") List<StorePerformanceResponse> topStores,
            @Schema(description = "Catalog distribution by card game") List<GameCountResponse> catalogByGame
    ) {}

    @Schema(description = "Super admin view of a catalog product listing")
    public record CatalogProductResponse(
            @Schema(description = "Product ID", example = "42") Integer id,
            @Schema(description = "Product name", example = "Charizard VMAX Shiny") String name,
            @Schema(description = "Card game", example = "Pokemon") String game,
            @Schema(description = "Card type", example = "Single") String type,
            @Schema(description = "Expansion set", example = "Shining Fates") String productSet,
            @Schema(description = "Card language", example = "English") String language,
            @Schema(description = "Cost basis in THB", example = "300.00") Double cost,
            @Schema(description = "Selling price in THB", example = "550.00") Double price,
            @Schema(description = "Available stock", example = "10") Integer stock,
            @Schema(description = "Product image URL") String imageUrl,
            @Schema(description = "Product description") String description,
            @Schema(description = "Active listing status", example = "true") Boolean active,
            @Schema(description = "Listing origin (OFFICIAL, MARKETPLACE)", example = "MARKETPLACE") String listingSource,
            @Schema(description = "Approval status", example = "APPROVED") String approvalStatus,
            @Schema(description = "Owning store ID", example = "3") Integer storeId,
            @Schema(description = "Owning store name", example = "Kanto Card Shop") String storeName
    ) {}

    @Schema(description = "Deep-dive merchant store details with product inventory")
    public record StoreResponse(
            @Schema(description = "Store ID", example = "3") Integer storeId,
            @Schema(description = "Store name", example = "Kanto Card Shop") String storeName,
            @Schema(description = "Store URL slug", example = "kanto-cards") String storeSlug,
            @Schema(description = "Verification status", example = "APPROVED") String storeStatus,
            @Schema(description = "Store description") String description,
            @Schema(description = "Owner full name", example = "Somchai Prasert") String ownerName,
            @Schema(description = "Owner email") String ownerEmail,
            @Schema(description = "Owner phone") String ownerPhone,
            @Schema(description = "Store location / city", example = "Pathum Wan, Bangkok") String location,
            @Schema(description = "Application submission timestamp") LocalDateTime submittedAt,
            @Schema(description = "Review timestamp") LocalDateTime reviewedAt,
            @Schema(description = "Total product count", example = "18") int productCount,
            @Schema(description = "Active product count", example = "15") int activeProductCount,
            @Schema(description = "In-stock product count", example = "12") int inStockProductCount,
            @Schema(description = "Total inventory units", example = "150") int totalStock,
            @Schema(description = "Fulfilled order count", example = "52") int orderCount,
            @Schema(description = "Gross Merchandise Value in THB", example = "125000.00") double gmv,
            @Schema(description = "Store product listings") List<CatalogProductResponse> products
    ) {}

    @Schema(description = "Financial transaction audit record")
    public record TransactionResponse(
            @Schema(description = "Order record ID", example = "201") Integer id,
            @Schema(description = "Order number", example = "ORD-20261111-9871") String orderNumber,
            @Schema(description = "Transaction timestamp") LocalDateTime createdAt,
            @Schema(description = "Buyer user ID", example = "101") Integer buyerId,
            @Schema(description = "Buyer display name", example = "kaiba_corp") String buyerName,
            @Schema(description = "Fulfilling store ID", example = "3") Integer storeId,
            @Schema(description = "Fulfilling store name", example = "Kanto Card Shop") String storeName,
            @Schema(description = "Transaction origin (OFFICIAL, MARKETPLACE)", example = "MARKETPLACE") String source,
            @Schema(description = "Order status", example = "DELIVERED") String status,
            @Schema(description = "Payment status", example = "PAID") String paymentStatus,
            @Schema(description = "Shipping method", example = "EXPRESS") String shippingMethod,
            @Schema(description = "Transaction total in THB", example = "1050.00") Double total,
            @Schema(description = "Number of items in order", example = "3") int itemCount
    ) {}

    @Schema(description = "Platform user account information")
    public record UserResponse(
            @Schema(description = "User ID", example = "101") Integer id,
            @Schema(description = "Username", example = "kaiba_corp") String username,
            @Schema(description = "Email address", example = "seto@kaiba.com") String email,
            @Schema(description = "Assigned role (USER, SELLER, ADMIN, SUPER_ADMIN)", example = "SELLER") String role,
            @Schema(description = "Phone number", example = "+66812345678") String phone,
            @Schema(description = "Address") String address,
            @Schema(description = "Account creation timestamp") LocalDateTime createdAt,
            @Schema(description = "Linked store ID (if seller)", example = "3") Integer storeId,
            @Schema(description = "Linked store name", example = "Kanto Card Shop") String storeName,
            @Schema(description = "Store verification status", example = "APPROVED") String storeStatus
    ) {}

    @Schema(description = "Administrator staff account information")
    public record StaffResponse(
            @Schema(description = "User ID", example = "1") Integer id,
            @Schema(description = "Username", example = "admin_ops") String username,
            @Schema(description = "Email address", example = "admin@optracard.com") String email,
            @Schema(description = "Role (ADMIN, SUPER_ADMIN)", example = "ADMIN") String role,
            @Schema(description = "Phone number") String phone,
            @Schema(description = "Address") String address,
            @Schema(description = "Account creation timestamp") LocalDateTime createdAt
    ) {}

    /** Super admins create standard ADMIN accounts; super-admin elevation remains a database-only action. */
    @Schema(description = "Payload to create a new ADMIN staff account")
    public record CreateStaffRequest(
            @Schema(description = "Staff username", example = "new_admin") String username,
            @Schema(description = "Staff email", example = "newadmin@optracard.com") String email,
            @Schema(description = "Initial password", example = "SecureAdminPass2026!") String password,
            @Schema(description = "Phone number", example = "+66891234567") String phone,
            @Schema(description = "Address") String address
    ) {}

    /** Required by the API in addition to the two-step confirmation presented in the workspace. */
    @Schema(description = "Deletion confirmation payload to prevent accidental removals")
    public record DeleteAccountRequest(
            @Schema(description = "Must be the literal string 'DELETE' to confirm", example = "DELETE") String confirmation
    ) {}
}
