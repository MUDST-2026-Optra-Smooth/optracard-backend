package org.example.config;

import org.example.model.CardGame;
import org.example.model.Product;
import org.example.repository.CardGameRepository;
import org.example.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyCardGameNormalizerTest {

    @Mock private ProductRepository productRepository;
    @Mock private CardGameRepository cardGameRepository;

    @Test
    void mergeLegacyGame_movesListingsToCanonicalGameThenRemovesTheDuplicate() {
        CardGame canonical = game(1, "Pokemon");
        CardGame legacy = game(2, "Pokemon TCG");
        Product legacyListing = new Product();
        legacyListing.setProId(88);
        legacyListing.setGameId(2);

        when(cardGameRepository.findByGameNameIgnoreCase("Pokemon TCG")).thenReturn(Optional.of(legacy));
        when(productRepository.findByGameId(2)).thenReturn(List.of(legacyListing));

        LegacyCardGameNormalizer.merge("Pokemon TCG", canonical, cardGameRepository, productRepository);

        assertEquals(1, legacyListing.getGameId());
        verify(productRepository).saveAll(List.of(legacyListing));
        verify(productRepository).flush();
        verify(cardGameRepository).delete(legacy);
        verify(cardGameRepository).flush();
    }

    private CardGame game(int id, String name) {
        CardGame game = new CardGame();
        game.setGameId(id);
        game.setGameName(name);
        return game;
    }
}
