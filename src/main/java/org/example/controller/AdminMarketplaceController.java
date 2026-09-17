package org.example.controller;

import org.example.dto.SellerProductResponse;
import org.example.model.Product;
import org.example.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/marketplace/requests")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminMarketplaceController {
    private final ProductRepository productRepository;

    public AdminMarketplaceController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<SellerProductResponse> list(Authentication authentication,
                                             @RequestParam(defaultValue = "ALL") String status) {
        requireAdmin(authentication);
        return productRepository.findByListingSourceOrderByProIdDesc("MARKETPLACE").stream()
                .filter(product -> "ALL".equalsIgnoreCase(status)
                        || status.equalsIgnoreCase(product.getApprovalStatus()))
                .map(this::toResponse)
                .toList();
    }

    @PutMapping("/{id}/approve")
    public SellerProductResponse approve(Authentication authentication, @PathVariable Integer id) {
        requireAdmin(authentication);
        Product product = findProduct(id);
        product.setApprovalStatus("APPROVED");
        product.setIsActive(true);
        return toResponse(productRepository.saveAndFlush(product));
    }

    @PutMapping("/{id}/reject")
    public SellerProductResponse reject(Authentication authentication,
                                        @PathVariable Integer id,
                                        @RequestBody(required = false) ReviewRequest request) {
        requireAdmin(authentication);
        Product product = findProduct(id);
        product.setApprovalStatus("REJECTED");
        product.setIsActive(false);
        return toResponse(productRepository.saveAndFlush(product));
    }

    private Product findProduct(Integer id) {
        Product product = productRepository.findByProId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Marketplace request not found"));
        if (!"MARKETPLACE".equalsIgnoreCase(product.getListingSource())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only marketplace products can be reviewed");
        }
        return product;
    }

    private void requireAdmin(Authentication authentication) {
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_SUPER_ADMIN"));
        if (!admin) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required");
    }

    private SellerProductResponse toResponse(Product product) {
        return new SellerProductResponse(product.getProId(), product.getProName(),
                product.getCardGame() == null ? "Uncategorized" : product.getCardGame().getGameName(),
                product.getProType(), product.getProCost(), product.getProPriceOfSell(), product.getProQuantity(),
                product.getProImageUrl(), product.getProductSet(), product.getLanguage(), product.getProDescription(),
                product.getApprovalStatus(), product.getIsActive(),
                product.getStore() == null ? null : product.getStore().getStoreId(),
                product.getStore() == null ? "Unknown store" : product.getStore().getStoreName());
    }

    public record ReviewRequest(String note) {}
}
