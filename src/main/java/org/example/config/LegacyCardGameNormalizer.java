package org.example.config;

import org.example.model.CardGame;
import org.example.model.Product;
import org.example.repository.CardGameRepository;
import org.example.repository.ProductRepository;

import java.util.List;

/** Keeps legacy game labels from creating duplicate choices in the catalog. */
final class LegacyCardGameNormalizer {
    private LegacyCardGameNormalizer() {}

    static void merge(String legacyName,
                      CardGame canonicalGame,
                      CardGameRepository cardGameRepository,
                      ProductRepository productRepository) {
        cardGameRepository.findByGameNameIgnoreCase(legacyName)
                .filter(legacyGame -> !legacyGame.getGameId().equals(canonicalGame.getGameId()))
                .ifPresent(legacyGame -> {
                    List<Product> products = productRepository.findByGameId(legacyGame.getGameId());
                    products.forEach(product -> product.setGameId(canonicalGame.getGameId()));
                    productRepository.saveAll(products);
                    productRepository.flush();
                    cardGameRepository.delete(legacyGame);
                    cardGameRepository.flush();
                });
    }
}
