package org.example.dto;
import java.util.List;
public record CatalogFilterOptionsResponse(List<String> types, List<GameOption> games, List<String> languages, List<String> sources, List<StoreOption> stores, PriceRange price) {
 public record GameOption(Integer id, String name) {}
 public record StoreOption(Integer id, String name) {}
 public record PriceRange(Double min, Double max) {}
}
