package org.example.controller;

import org.example.dto.StockAlertResponse;
import org.example.dto.StockAlertSettingsRequest;
import org.example.dto.StockAlertSettingsResponse;
import org.example.service.StockAlertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockAlertControllerTest {

    @Mock
    private StockAlertService stockAlertService;

    @InjectMocks
    private StockAlertController controller;

    private final Authentication auth = new UsernamePasswordAuthenticationToken("seller@example.com", "pass");

    @Test
    @DisplayName("alerts returns list of alerts for authenticated user")
    void alerts_success() {
        StockAlertResponse alert = mock(StockAlertResponse.class);
        when(stockAlertService.getAlerts("seller@example.com")).thenReturn(List.of(alert));

        ResponseEntity<List<StockAlertResponse>> result = controller.alerts(auth);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("no-store", result.getHeaders().getCacheControl());
        assertEquals(List.of(alert), result.getBody());
        verify(stockAlertService).getAlerts("seller@example.com");
    }

    @Test
    @DisplayName("markAllRead returns 204 No Content")
    void markAllRead_success() {
        ResponseEntity<Void> response = controller.markAllRead(auth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(stockAlertService).markAllRead("seller@example.com");
    }

    @Test
    @DisplayName("markRead and markUnread return 204 No Content with appropriate boolean flag")
    void markReadAndUnread_success() {
        ResponseEntity<Void> readResponse = controller.markRead(auth, 7);
        assertEquals(HttpStatus.NO_CONTENT, readResponse.getStatusCode());
        verify(stockAlertService).markRead("seller@example.com", 7, true);

        ResponseEntity<Void> unreadResponse = controller.markUnread(auth, 7);
        assertEquals(HttpStatus.NO_CONTENT, unreadResponse.getStatusCode());
        verify(stockAlertService).markRead("seller@example.com", 7, false);
    }

    @Test
    @DisplayName("settings and updateSettings delegate to service")
    void settings_success() {
        StockAlertSettingsResponse settingsResp = mock(StockAlertSettingsResponse.class);
        StockAlertSettingsRequest req = mock(StockAlertSettingsRequest.class);

        when(stockAlertService.getSettings("seller@example.com")).thenReturn(settingsResp);
        when(stockAlertService.updateSettings("seller@example.com", req)).thenReturn(settingsResp);

        assertSame(settingsResp, controller.settings(auth));
        assertSame(settingsResp, controller.updateSettings(auth, req));

        verify(stockAlertService).getSettings("seller@example.com");
        verify(stockAlertService).updateSettings("seller@example.com", req);
    }
}

