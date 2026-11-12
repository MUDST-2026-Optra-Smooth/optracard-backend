package org.example.controller;

import org.example.dto.SettlementReportResponse;
import org.example.service.MarketplaceSettlementReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/seller/settlements")
@CrossOrigin(origins = "http://localhost:5173")
public class SellerSettlementController {
    private final MarketplaceSettlementReportService reportService;

    public SellerSettlementController(MarketplaceSettlementReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public SettlementReportResponse report(Authentication authentication,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           @RequestParam(required = false) String type) {
        return reportService.sellerReport(authentication == null ? null : authentication.getName(), from, to, type);
    }
}
