package org.example.controller;

import org.example.dto.AdminData;
import org.example.service.AdminDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminDataController {
    private final AdminDataService adminDataService;

    public AdminDataController(AdminDataService adminDataService) {
        this.adminDataService = adminDataService;
    }

    @GetMapping("/dashboard")
    public AdminData.DashboardResponse dashboard() { return adminDataService.getDashboard(); }

    @GetMapping("/card-games")
    public List<AdminData.CardGameResponse> cardGames() { return adminDataService.getCardGames(); }

    @GetMapping("/products")
    public List<AdminData.ProductResponse> officialProducts() { return adminDataService.getOfficialProducts(); }

    @GetMapping("/products/{id}")
    public AdminData.ProductResponse officialProduct(@PathVariable Integer id) { return adminDataService.getOfficialProduct(id); }

    @PostMapping("/products")
    public ResponseEntity<AdminData.ProductResponse> createOfficialProduct(@RequestBody AdminData.ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDataService.createOfficialProduct(request));
    }

    @PutMapping("/products/{id}")
    public AdminData.ProductResponse updateOfficialProduct(@PathVariable Integer id,
                                                           @RequestBody AdminData.ProductRequest request) {
        return adminDataService.updateOfficialProduct(id, request);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deactivateOfficialProduct(@PathVariable Integer id) {
        adminDataService.deactivateOfficialProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders")
    public List<AdminData.OrderResponse> orders() { return adminDataService.getOrders(); }

    @GetMapping("/orders/{id}")
    public AdminData.OrderResponse order(@PathVariable Integer id) { return adminDataService.getOrder(id); }

    @PutMapping("/orders/{id}/status")
    public AdminData.OrderResponse updateOrderStatus(@PathVariable Integer id,
                                                      @RequestBody AdminData.OrderStatusRequest request) {
        return adminDataService.updateOrderStatus(id, request == null ? null : request.status());
    }

    @GetMapping("/stores")
    public List<AdminData.StoreResponse> stores(@RequestParam(defaultValue = "ALL") String status) {
        return adminDataService.getStores(status);
    }

    @GetMapping("/stores/{id}")
    public AdminData.StoreResponse store(@PathVariable Integer id) { return adminDataService.getStore(id); }

    @GetMapping("/marketplace/products")
    public List<AdminData.ProductResponse> marketplaceProducts(@RequestParam(required = false) Integer storeId) {
        return adminDataService.getMarketplaceProducts(storeId);
    }

    @GetMapping("/marketplace/products/{id}")
    public AdminData.ProductResponse marketplaceProduct(@PathVariable Integer id) {
        return adminDataService.getMarketplaceProduct(id);
    }
}
