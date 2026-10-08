package org.example.service;

import org.example.dto.CatalogProductResponse;
import org.example.model.CardGame;
import org.example.model.MarketplaceStore;
import org.example.model.Product;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MarketplaceStoreRepository marketplaceStoreRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, marketplaceStoreRepository);
    }

    @Test
    void searchProducts_whenEmptyKeyword_returnsAllProducts() {
        Product p = new Product();
        p.setProId(1);
        p.setProName("Test Card");
        p.setIsActive(true);
        p.setListingSource("OFFICIAL");
        when(productRepository.findByIsActiveTrueOrderByProTypeAscProIdAsc()).thenReturn(List.of(p));

        List<CatalogProductResponse> results = productService.searchProducts("");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Test Card", results.get(0).name());
        verify(productRepository).findByIsActiveTrueOrderByProTypeAscProIdAsc();
    }

    @Test
    void searchProducts_whenKeywordProvided_searchesByKeyword() {
        Product p = new Product();
        p.setProId(2);
        p.setProName("Charizard");
        p.setIsActive(true);
        p.setListingSource("OFFICIAL");
        when(productRepository.searchByKeyword("Charizard")).thenReturn(List.of(p));

        List<CatalogProductResponse> results = productService.searchProducts(" Charizard ");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Charizard", results.get(0).name());
        verify(productRepository).searchByKeyword("Charizard");
    }

    @Test
    void getProductOffers_returnsOfficialAndMarketplaceListingsForTheSameItem() {
        CardGame onePiece = new CardGame();
        onePiece.setGameId(1);
        onePiece.setGameName("One Piece");

        Product official = new Product();
        official.setProId(10);
        official.setProName("One Piece OP-10 Royal Blood Booster");
        official.setProType("Booster");
        official.setGameId(1);
        official.setCardGame(onePiece);
        official.setProductSet("Royal Blood [OP-10]");
        official.setLanguage("Japanese");
        official.setProPriceOfSell(159.0);
        official.setProQuantity(28);
        official.setIsActive(true);
        official.setListingSource("OFFICIAL");

        MarketplaceStore sellerStore = new MarketplaceStore();
        sellerStore.setStoreId(2);
        sellerStore.setStoreName("AAA-Trading");
        sellerStore.setStoreSlug("aaa-trading");
        sellerStore.setStoreStatus("APPROVED");

        Product marketplace = new Product();
        marketplace.setProId(11);
        marketplace.setProName("One Piece OP-10 Royal Blood Booster");
        marketplace.setProType("Booster");
        marketplace.setGameId(1);
        marketplace.setCardGame(onePiece);
        marketplace.setProductSet("Royal Blood [OP-10]");
        marketplace.setLanguage("Japanese");
        marketplace.setProPriceOfSell(145.0);
        marketplace.setProQuantity(20);
        marketplace.setIsActive(true);
        marketplace.setListingSource("MARKETPLACE");
        marketplace.setApprovalStatus("APPROVED");
        marketplace.setStore(sellerStore);

        when(productRepository.findByProId(10)).thenReturn(Optional.of(official));
        when(productRepository.findByProNameIgnoreCaseAndProTypeAndGameIdAndIsActiveTrueOrderByProPriceOfSellAscProIdAsc(
                "One Piece OP-10 Royal Blood Booster", "Booster", 1)).thenReturn(List.of(marketplace, official));

        List<CatalogProductResponse> offers = productService.getProductOffers(10).orElseThrow();

        assertEquals(2, offers.size());
        assertEquals("MARKETPLACE", offers.get(0).source());
        assertEquals("AAA-Trading", offers.get(0).store().name());
        assertEquals("OFFICIAL", offers.get(1).source());
    }
}
