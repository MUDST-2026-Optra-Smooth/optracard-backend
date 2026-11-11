package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Trading card game option")
public record CardGameOptionResponse(
        @Schema(description = "Game ID", example = "1")
        Integer id,

        @Schema(description = "Game display title", example = "Pokemon")
        String name
) {}
