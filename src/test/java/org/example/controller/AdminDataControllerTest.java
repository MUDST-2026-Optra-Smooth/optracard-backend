package org.example.controller;

import org.example.dto.AdminData;
import org.example.service.AdminDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDataControllerTest {

    @Mock
    private AdminDataService adminDataService;

    @InjectMocks
    private AdminDataController adminDataController;

    private AdminData.DashboardResponse mockDashboard;

    @BeforeEach
    void setUp() {
        mockDashboard = new AdminData.DashboardResponse(
                10000.0, 4000.0, 6000.0, 50, 100,
                200.0, 20, 15,
                2, 3, 5,
                List.of(), List.of()
        );
    }

    @Test
    @DisplayName("dashboard() delegates to AdminDataService")
    void dashboard_returnsDashboardMetrics() {
        when(adminDataService.getDashboard()).thenReturn(mockDashboard);

        AdminData.DashboardResponse result = adminDataController.dashboard();

        assertNotNull(result);
        assertEquals(10000.0, result.totalRevenue());
        verify(adminDataService).getDashboard();
    }

    @Test
    @DisplayName("cardGames() returns card game list")
    void cardGames_returnsCardGamesList() {
        AdminData.CardGameResponse game = new AdminData.CardGameResponse(1, "Pokemon");
        when(adminDataService.getCardGames()).thenReturn(List.of(game));

        List<AdminData.CardGameResponse> result = adminDataController.cardGames();

        assertEquals(1, result.size());
        assertEquals("Pokemon", result.get(0).name());
        verify(adminDataService).getCardGames();
    }

    @Test
    @DisplayName("createOfficialProduct() returns 201 Created")
    void createOfficialProduct_returnsCreated() {
        AdminData.ProductRequest req = new AdminData.ProductRequest(
                "Card A", "Pokemon", "Single", 10.0, 20.0, 5, "URL", "Set A", "EN", "Desc"
        );
        AdminData.ProductResponse res = new AdminData.ProductResponse(
                1, "Card A", "Pokemon", "Single", 10.0, 20.0, 5, "URL", "Set A", "EN", "Desc", true, "OFFICIAL", "APPROVED", null, null
        );
        when(adminDataService.createOfficialProduct(req)).thenReturn(res);

        ResponseEntity<AdminData.ProductResponse> response = adminDataController.createOfficialProduct(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(res, response.getBody());
        verify(adminDataService).createOfficialProduct(req);
    }

    @Test
    @DisplayName("deleteOfficialProduct() delegates and returns 204 No Content")
    void deleteOfficialProduct_returnsNoContent() {
        doNothing().when(adminDataService).deleteOfficialProduct(1);

        ResponseEntity<Void> response = adminDataController.deleteOfficialProduct(1);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(adminDataService).deleteOfficialProduct(1);
    }
}

