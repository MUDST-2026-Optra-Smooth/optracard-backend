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
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

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
        when(productRepository.findByIsActiveTrueOrderByProTypeAscProIdAsc()).thenReturn(List.of(p));

        List<CatalogProductResponse> results = productService.searchProducts(" Charizard ");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Charizard", results.get(0).name());
        verify(productRepository).findByIsActiveTrueOrderByProTypeAscProIdAsc();
    }

    @Test
    void searchProducts_ranksExactAndHidesUnapprovedListings() {
        Product exact = new Product();
        exact.setProId(1);
        exact.setProName("Pokemon Card");
        exact.setIsActive(true);
        Product fuzzy = new Product();
        fuzzy.setProId(2);
        fuzzy.setProName("Pokamon Card");
        fuzzy.setIsActive(true);
        Product hidden = new Product();
        hidden.setProId(3);
        hidden.setProName("Pokemon hidden");
        hidden.setIsActive(true);
        hidden.setListingSource("MARKETPLACE");
        hidden.setApprovalStatus("PENDING");
        when(productRepository.findByIsActiveTrueOrderByProTypeAscProIdAsc()).thenReturn(List.of(fuzzy, hidden, exact));

        assertEquals(List.of(1, 2), productService.searchProducts("pokemon").stream()
                .map(CatalogProductResponse::id).toList());
    }

    @Test
    void catalog_usesTolerantMatchingForTyposAndThaiAliases() {
        CardGame pokemon = new CardGame();
        pokemon.setGameName("Pokemon");

        Product matched = new Product();
        matched.setProId(1);
        matched.setProName("Pokemon Card");
        matched.setProType("Single");
        matched.setCardGame(pokemon);
        matched.setProPriceOfSell(100.0);
        matched.setProQuantity(1);
        matched.setIsActive(true);
        matched.setListingSource("OFFICIAL");

        Product unrelated = new Product();
        unrelated.setProId(2);
        unrelated.setProName("Blue-Eyes White Dragon");
        unrelated.setProType("Single");
        unrelated.setIsActive(true);
        unrelated.setListingSource("OFFICIAL");

        when(productRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(matched, unrelated));

        assertEquals(List.of(1), productService.catalog("pokamon", null, null, null, null,
                        null, null, null, "newest", 0, 24).items().stream()
                .map(CatalogProductResponse::id).toList());
        assertEquals(List.of(1), productService.catalog("โปเกมอน", null, null, null, null,
                        null, null, null, "newest", 0, 24).items().stream()
                .map(CatalogProductResponse::id).toList());
    }

    @Test
    void searchProducts_onlyReturnsActiveApprovedMarketplaceProductsFromApprovedStores() {
        MarketplaceStore approved = new MarketplaceStore();
        approved.setStoreName("Card Corner");
        approved.setStoreStatus("APPROVED");
        MarketplaceStore pendingStore = new MarketplaceStore();
        pendingStore.setStoreName("Card Corner");
        pendingStore.setStoreStatus("PENDING");
        Product visible = marketplaceProduct(1, approved, "APPROVED", true);
        Product pendingListing = marketplaceProduct(2, approved, "PENDING", true);
        Product pendingSeller = marketplaceProduct(3, pendingStore, "APPROVED", true);
        Product inactive = marketplaceProduct(4, approved, "APPROVED", false);
        when(productRepository.findByIsActiveTrueOrderByProTypeAscProIdAsc())
                .thenReturn(List.of(visible, pendingListing, pendingSeller, inactive));

        assertEquals(List.of(1), productService.searchProducts("card corner").stream()
                .map(CatalogProductResponse::id).toList());
    }

    private Product marketplaceProduct(int id, MarketplaceStore store, String approval, boolean active) {
        Product product = new Product();
        product.setProId(id);
        product.setProName("Pokemon Card");
        product.setListingSource("MARKETPLACE");
        product.setApprovalStatus(approval);
        product.setIsActive(active);
        product.setStore(store);
        return product;
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

    @Test
    void publicCatalogOperations_coverVisibilityProfilesFiltersAndPaging() {
        CardGame game = new CardGame();
        game.setGameId(4);
        game.setGameName("Pokemon");
        MarketplaceStore approved = new MarketplaceStore();
        approved.setStoreId(8);
        approved.setStoreName("Approved Store");
        approved.setStoreSlug("approved-store");
        approved.setStoreStatus("APPROVED");
        approved.setStoreDescription("Cards");
        approved.setDistrict("Bang Rak");
        approved.setProvince("Bangkok");

        Product official = new Product();
        official.setProId(20); official.setProName("Pikachu"); official.setProType("Single");
        official.setCardGame(game); official.setProPriceOfSell(99.0); official.setProQuantity(2);
        official.setIsActive(true); official.setListingSource("OFFICIAL"); official.setLanguage("English");
        Product marketplace = marketplaceProduct(21, approved, "APPROVED", true);
        marketplace.setCardGame(game); marketplace.setProType("Single"); marketplace.setProPriceOfSell(120.0);
        Product hidden = marketplaceProduct(22, approved, "PENDING", true);
        when(productRepository.findByIsActiveTrueOrderByProTypeAscProIdAsc()).thenReturn(List.of(official, marketplace, hidden));
        when(productRepository.findByProId(20)).thenReturn(Optional.of(official));
        when(productRepository.findByProId(22)).thenReturn(Optional.of(hidden));
        assertEquals(2, productService.getAllProducts().size());
        assertEquals("Pikachu", productService.getProductById(20).orElseThrow().name());
        assertEquals(0, productService.getProductById(22).stream().count());

        when(marketplaceStoreRepository.findByStoreId(8)).thenReturn(Optional.of(approved));
        when(productRepository.findByStore_StoreIdOrderByProIdDesc(8)).thenReturn(List.of(marketplace, hidden));
        var profile = productService.getMarketplaceStoreProfile(8).orElseThrow();
        assertEquals("Bang Rak, Bangkok", profile.location());
        assertEquals(1, profile.products().size());

        when(productRepository.findPublicCatalogTypes()).thenReturn(Arrays.asList("Single", null, "Booster"));
        when(productRepository.findPublicCatalogGames()).thenReturn(List.<Object[]>of(new Object[]{2, "One Piece"}, new Object[]{1, "Pokemon"}));
        when(productRepository.findPublicCatalogLanguages()).thenReturn(Arrays.asList("Japanese", null));
        when(productRepository.findPublicCatalogSources()).thenReturn(Arrays.asList("MARKETPLACE", null));
        when(productRepository.findPublicCatalogStores()).thenReturn(List.<Object[]>of(new Object[]{8, "Approved Store"}));
        when(productRepository.findPublicCatalogPriceRange()).thenReturn(new Object[]{10, 500});
        assertEquals(2, productService.filterOptions().types().size());

        var page = new PageImpl<>(List.of(official));
        when(productRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        assertEquals(1, productService.catalog(null, null, null, null, null, null, null, null, "newest", 0, 24).items().size());
        Product saved = new Product();
        when(productRepository.save(saved)).thenReturn(saved);
        assertEquals(saved, productService.createProduct(saved));
    }
}
