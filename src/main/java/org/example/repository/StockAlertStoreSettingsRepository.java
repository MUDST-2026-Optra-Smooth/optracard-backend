package org.example.repository;

import org.example.model.StockAlertStoreSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAlertStoreSettingsRepository extends JpaRepository<StockAlertStoreSettings, Integer> {
}
