package org.example.controller;

import org.example.dto.MarketplaceFeeConfigResponse;
import org.example.dto.UpdateMarketplaceFeeRequest;
import org.example.service.MarketplaceFeeService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin/marketplace-fee")
@CrossOrigin(origins = "http://localhost:5173")
public class MarketplaceFeeController {
    private final MarketplaceFeeService feeService;

    public MarketplaceFeeController(MarketplaceFeeService feeService) {
        this.feeService = feeService;
    }

    @GetMapping("/config")
    public MarketplaceFeeConfigResponse currentConfiguration() {
        return feeService.currentResponse();
    }

    @GetMapping("/config/history")
    public List<MarketplaceFeeConfigResponse> history() {
        return feeService.history();
    }

    @PutMapping("/config")
    public MarketplaceFeeConfigResponse updateConfiguration(Authentication authentication,
                                                            @RequestBody UpdateMarketplaceFeeRequest request) {
        return feeService.update(request, authentication == null ? null : authentication.getName());
    }
}
