package org.example.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogSearchMatcherTest {
    private int match(String query, String name, String game) {
        return CatalogSearchMatcher.score(query, name, game, "Single", "", "");
    }

    @Test
    void normalizesAccentsCaseAndThaiSynonyms() {
        assertEquals(0, match("  POKEMON  ", "Pokémon Card", "Pokémon"));
        assertEquals(0, match("โปเกมอน booster", "Booster Box", "Pokemon"));
        assertEquals(0, match("วันพีซ", "Luffy", "One Piece"));
        assertEquals("โปเกมอน", CatalogSearchMatcher.normalize("โปเกมอน"));
    }

    @Test
    void acceptsOnlySmallMistakesInNamesAndGames() {
        assertEquals(1, match("pokamon", "Card", "Pokemon"));
        assertEquals(CatalogSearchMatcher.NO_MATCH, match("pokaxxn", "Card", "Pokemon"));
        assertEquals(CatalogSearchMatcher.NO_MATCH, match("abx", "abc", ""));
        assertEquals(2, match("chaxxzard", "Charizard", ""));
        assertEquals(CatalogSearchMatcher.NO_MATCH, CatalogSearchMatcher.score("sellar", "Card", "Game", "", "", "seller"));
    }

    @Test
    void searchesOtherFieldsExactly() {
        assertEquals(0, CatalogSearchMatcher.score("trading", "Card", "Game", "", "", "AAA Trading"));
        assertEquals(0, CatalogSearchMatcher.score("booster", "Card", "Game", "Booster Box", "", ""));
    }
}
