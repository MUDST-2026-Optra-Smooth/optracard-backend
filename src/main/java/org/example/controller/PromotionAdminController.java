package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.PromotionRequest;
import org.example.dto.PromotionResponse;
import org.example.service.PromotionService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/promotions")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Admin Promotions", description = "Administrative endpoints for coupon codes, discount campaigns, and promotional rules")
@SecurityRequirement(name = "bearerAuth")
public class PromotionAdminController {
    private final PromotionService promotionService;

    public PromotionAdminController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @Operation(
            summary = "List all promotions",
            description = "Retrieve all discount codes, configured values, active dates, and redemption counts."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotions list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority")
    })
    @GetMapping
    public List<PromotionResponse> promotions() { return promotionService.getPromotions(); }

    @Operation(
            summary = "Get promotion by ID",
            description = "Retrieve details and constraints of a specific promotional coupon."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotion details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "Promotion not found")
    })
    @GetMapping("/{id}")
    public PromotionResponse promotion(
            @Parameter(description = "Promotion ID", example = "1") @PathVariable Integer id) {
        return promotionService.getPromotion(id);
    }

    @Operation(
            summary = "Create promotion coupon",
            description = "Create a new coupon code with percentage or fixed discount, usage limits, and active date range."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Promotion created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid dates, negative discount, or duplicate coupon code"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority")
    })
    @PostMapping
    public ResponseEntity<PromotionResponse> create(@RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.createPromotion(request));
    }

    @Operation(
            summary = "Update promotion coupon",
            description = "Update an existing promotional coupon's parameters, active dates, or limits."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotion updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid promotion update parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "Promotion not found")
    })
    @PutMapping("/{id}")
    public PromotionResponse update(
            @Parameter(description = "Promotion ID", example = "1") @PathVariable Integer id,
            @RequestBody PromotionRequest request) {
        return promotionService.updatePromotion(id, request);
    }

    @Operation(
            summary = "Delete promotion coupon",
            description = "Permanently delete a promotional coupon code."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Promotion deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "Promotion not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Promotion ID", example = "1") @PathVariable Integer id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }
}
