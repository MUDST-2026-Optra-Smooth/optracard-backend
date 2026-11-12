package org.example.dto;

import java.util.List;

public record SettlementReportResponse(
        SettlementSummaryResponse summary,
        List<SettlementLedgerEntryResponse> entries
) {}
