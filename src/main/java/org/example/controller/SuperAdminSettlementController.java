package org.example.controller;

import org.example.dto.SettlementReportResponse;
import org.example.service.MarketplaceSettlementReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/superadmin/settlements")
@CrossOrigin(origins = "http://localhost:5173")
public class SuperAdminSettlementController {
    private final MarketplaceSettlementReportService reportService;

    public SuperAdminSettlementController(MarketplaceSettlementReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public SettlementReportResponse report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer sellerUserId,
            @RequestParam(required = false) Integer storeId) {
        return reportService.superAdminReport(from, to, type, sellerUserId, storeId);
    }
}
