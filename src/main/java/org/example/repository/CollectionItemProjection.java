package org.example.repository;

/** Projection returned by the user's purchased-card collection query. */
public interface CollectionItemProjection {
    Integer getProductId();

    String getName();

    String getGame();

    String getImageUrl();

    Long getQuantity();
}
