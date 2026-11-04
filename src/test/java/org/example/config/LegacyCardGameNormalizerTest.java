package org.example.config;

import org.example.model.CardGame;
import org.example.model.Product;
import org.example.repository.CardGameRepository;
import org.example.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegacyCardGameNormalizerTest {

    @Mock
    private CardGameRepository cardGameRepository;

    @Mock
    private ProductRepository productRepository;

    @Test
    @DisplayName("Should remap products to canonical game ID and delete legacy game when found with different ID")
    void merge_whenLegacyExistsAndDifferentId_remapsProductsAndDeletesLegacy() {
        CardGame canonical = new CardGame();
        canonical.setGameId(10);
        canonical.setGameName("Magic The Gathering");

        CardGame legacy = new CardGame();
        legacy.setGameId(20);
        legacy.setGameName("MTG");

        Product p1 = new Product();
        p1.setProId(1);
        p1.setGameId(20);

        Product p2 = new Product();
        p2.setProId(2);
        p2.setGameId(20);

        List<Product> products = new ArrayList<>(List.of(p1, p2));

        when(cardGameRepository.findByGameNameIgnoreCase("mtg")).thenReturn(Optional.of(legacy));
        when(productRepository.findByGameId(20)).thenReturn(products);

        LegacyCardGameNormalizer.merge("mtg", canonical, cardGameRepository, productRepository);

        assertEquals(10, p1.getGameId());
        assertEquals(10, p2.getGameId());

        verify(productRepository).saveAll(products);
        verify(productRepository).flush();
        verify(cardGameRepository).delete(legacy);
        verify(cardGameRepository).flush();
    }

    @Test
    @DisplayName("Should do nothing when legacy game has identical ID to canonical game")
    void merge_whenLegacyHasSameIdAsCanonical_doesNothing() {
        CardGame canonical = new CardGame();
        canonical.setGameId(10);
        canonical.setGameName("Pokemon");

        CardGame legacy = new CardGame();
        legacy.setGameId(10);
        legacy.setGameName("Pokemon");

        when(cardGameRepository.findByGameNameIgnoreCase("pokemon")).thenReturn(Optional.of(legacy));

        LegacyCardGameNormalizer.merge("pokemon", canonical, cardGameRepository, productRepository);

        verify(productRepository, never()).findByGameId(anyInt());
        verify(productRepository, never()).saveAll(any());
        verify(cardGameRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should do nothing when legacy game is not found")
    void merge_whenLegacyNotFound_doesNothing() {
        CardGame canonical = new CardGame();
        canonical.setGameId(10);
        canonical.setGameName("One Piece");

        when(cardGameRepository.findByGameNameIgnoreCase("onepiece")).thenReturn(Optional.empty());

        LegacyCardGameNormalizer.merge("onepiece", canonical, cardGameRepository, productRepository);

        verify(productRepository, never()).findByGameId(anyInt());
        verify(cardGameRepository, never()).delete(any());
    }
}
