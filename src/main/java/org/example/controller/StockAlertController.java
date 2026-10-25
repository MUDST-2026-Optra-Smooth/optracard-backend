package org.example.controller;

import org.example.dto.StockAlertResponse;
import org.example.dto.StockAlertSettingsRequest;
import org.example.dto.StockAlertSettingsResponse;
import org.example.service.StockAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock-alerts")
@CrossOrigin(origins = "http://localhost:5173")
public class StockAlertController {
    private final StockAlertService stockAlertService;

    public StockAlertController(StockAlertService stockAlertService) {
        this.stockAlertService = stockAlertService;
    }

    @GetMapping
    public List<StockAlertResponse> alerts(Authentication authentication) {
        return stockAlertService.getAlerts(authentication.getName());
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        stockAlertService.markAllRead(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(Authentication authentication, @PathVariable Integer id) {
        stockAlertService.markRead(authentication.getName(), id, true);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/unread")
    public ResponseEntity<Void> markUnread(Authentication authentication, @PathVariable Integer id) {
        stockAlertService.markRead(authentication.getName(), id, false);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings")
    public StockAlertSettingsResponse settings() {
        return stockAlertService.getSettings();
    }

    @PutMapping("/settings")
    public StockAlertSettingsResponse updateSettings(Authentication authentication,
                                                     @RequestBody StockAlertSettingsRequest request) {
        return stockAlertService.updateSettings(authentication.getName(), request);
    }
}
