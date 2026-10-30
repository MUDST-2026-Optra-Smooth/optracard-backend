package org.example.dto;
import java.util.List;
public record CatalogPageResponse(List<CatalogProductResponse> items, int page, int pageSize, long totalItems, int totalPages, String sort) {}
