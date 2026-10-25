package org.example.repository;

import org.example.model.StockAlertSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAlertSettingsRepository extends JpaRepository<StockAlertSettings, Integer> {
}
