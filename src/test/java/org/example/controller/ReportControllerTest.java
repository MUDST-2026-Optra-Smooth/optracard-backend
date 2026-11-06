package org.example.controller;

import org.example.dto.WeeklyReportResponse;
import org.example.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController controller;

    @Test
    @DisplayName("getWeeklyReport delegates to service and returns 200 OK")
    void getWeeklyReport_success() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 7);
        WeeklyReportResponse expected = new WeeklyReportResponse(
                BigDecimal.valueOf(5000), BigDecimal.valueOf(2000), BigDecimal.valueOf(3000),
                10L, 25L, List.of()
        );
        when(reportService.generateWeeklyReport(start, end)).thenReturn(expected);

        ResponseEntity<WeeklyReportResponse> response = controller.getWeeklyReport(start, end);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        assertEquals(BigDecimal.valueOf(5000), response.getBody().getRevenue());
        verify(reportService).generateWeeklyReport(start, end);
    }
}

