package org.example.config;

import org.example.model.CardGame;
import org.example.model.MarketplaceStore;
import org.example.model.Product;
import org.example.model.User;
import org.example.repository.CardGameRepository;
import org.example.repository.MarketplaceStoreRepository;
import org.example.repository.ProductRepository;
import org.example.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Local-only catalog data for the home page. Every SKU is checked before it is
 * inserted, so restarting the backend never duplicates the sample listings.
 */
@Component
@Profile("local")
public class LocalCatalogSeeder implements ApplicationRunner {

    private static final String OFFICIAL = "OFFICIAL";
    private static final String MARKETPLACE = "MARKETPLACE";

    // Product images are hosted by the retailers/card catalogs returned from
    // the web image search. They are used only for local demo catalog data.
    private static final String POKEMON_CARD_IMAGE = "https://storage.googleapis.com/images.pricecharting.com/7462a587edba78896c3989d02a492d54ce45ae920b56610d150ef238501900a2/1600.jpg";
    private static final String POKEMON_PACK_IMAGE = "https://www.cardsplace.de/cdn/shop/files/Pokemon_Trading_Card_Game_-_Surging_Sparks_Booster_Pack_SV8_-_Scarlet_Violet_8_Englisch.png?v=1727039941&width=533";
    private static final String POKEMON_BOX_IMAGE = "https://cdn.shopify.com/s/files/1/0817/1297/2120/files/Pokemon_Journey_Together_Booster_Box.png?v=1767965841&width=1920";
    private static final String ONE_PIECE_CARD_IMAGE = "https://www.torecolo.jp/img/goods/5/OP01-024SR.jpg";
    private static final String ONE_PIECE_PACK_IMAGE = "https://monketic.com/cdn/shop/files/PACK-OP13_1359457c-0192-4db6-998a-57b2096729b8.png?v=1756066509&width=533";
    private static final String YUGIOH_CARD_IMAGE = "https://tcgplayer-cdn.tcgplayer.com/product/22941_in_1000x1000.jpg";
    private static final String YUGIOH_BOX_IMAGE = "https://www.yugioh-card.com/en/wp-content/uploads/2025/09/ch02_tuckbox_550x550.png";
    private static final String MAGIC_IMAGE = "https://meccha-japan.com/797671-large_default/lorwyn-eclipsed-collector-booster-box-english-ver-magic-the-gathering.jpg";
    private static final String SLEEVES_IMAGE = "https://jollycards.fr/cdn/shop/files/SleevesUltraPro_1150x.png?v=1716543880";
    private static final String BINDER_IMAGE = "https://gemloader.com/cdn/shop/files/480_tsb_Gemloader_tiffany_blue_1_1080x.png?v=1773502942";
    private static final String PIKACHU_EX_IMAGE = "https://static.wixstatic.com/media/d6578e_45d1f9d28d3146f485b88d8ac82ca4bf~mv2.jpg/v1/fill/w_1080%2Ch_1080%2Cal_c%2Cq_85%2Cenc_avif%2Cquality_auto/d6578e_45d1f9d28d3146f485b88d8ac82ca4bf~mv2.jpg";
    private static final String LUFFY_LEADER_IMAGE = "https://media.okini.land/124052-medium_default/one-piece-cg-op01-l-op01-003-parallel-monkey-d-luffy.jpg";
    private static final String DARK_MAGICIAN_IMAGE = "https://cardbot.co.nz/cdn/shop/files/21767880-front.jpg?v=1741737540";
    private static final String LILIANA_IMAGE = "https://cdn11.bigcommerce.com/s-3b5vpig99v/images/stencil/1280x1280/products/576154/1187701/LilianaOfTheVeil373__07432.1681937228.jpg?c=2";
    private static final String JOURNEY_PACK_IMAGE = "https://www.binderly.co.uk/cdn/shop/files/PokemonTCG-Scarlet_Violet-JourneyTogether-BoosterPack.png?v=1753051152&width=1200";
    private static final String OP10_PACK_IMAGE = "https://cdn11.bigcommerce.com/s-b4ioc4fed9/images/stencil/1280x1280/products/514153/3136836/2DOwlfvOa3pbiGn0L7e261DgE__60896.1767665863.jpg?c=1";
    private static final String OP10_BOX_IMAGE = "https://tcgame.com.au/cdn/shop/files/one-piece-card-game-royal-blood-op-10-booster-box.jpg?v=1753268799&width=416";
    private static final String ALLIANCE_PACK_IMAGE = "https://www.boardgamesdallas.com/cdn/shop/files/86764-2__49848_1200x1200.webp?v=1746032074";
    private static final String ALLIANCE_BOX_IMAGE = "https://topshelfco.ca/cdn/shop/files/Yu-Gi-Oh_-_Alliance_Insight_-_Booster_Box.png?v=1748701515";
    private static final String AETHERDRIFT_PACK_IMAGE = "https://cdn.shoplightspeed.com/shops/636231/files/67795933/1652x1652x2/magic-the-gathering-mtg-aetherdrift-play-booster-p.jpg";
    private static final String AETHERDRIFT_BOX_IMAGE = "https://shop.threeforonetrading.com/cdn/shop/files/Aetherdrift-Play-Booster-Box-English.jpg?v=1740328950";
    private static final String GENGAR_IMAGE = "https://store.401games.ca/cdn/shop/files/Gengar-VMAX-271264-Alternate-Art-Secret-Rare_498x.jpg?v=1698582538";
    private static final String NAMI_IMAGE = "https://mintcollectables.com.au/cdn/shop/files/021324-1990-Fleer5096F.jpg?v=1750132610";
    private static final String BLUE_EYES_IMAGE = "https://images.ygoprodeck.com/images/cards/89631139.jpg";
    private static final String MANA_CRYPT_IMAGE = "https://facetofacegames.com/cdn/shop/files/39ce7d2c982b01a0a55c87f7e2beadf74e514e28_Asset_MTG_2XM_361_ENG_F_jpg.jpg?v=1737426116&width=1445";
    private static final String POKEMON_151_KOREAN_IMAGE = "https://spoilsandloot.com/cdn/shop/files/Korean_Pokemon_151_Booster_Box_TCG_Pack.webp?v=1769982189&width=1946";
    private static final String PRB01_PACK_IMAGE = "https://cultcollectables.com/cdn/shop/files/one-piece-tcg-the-best-prb-01-japanese-booster-pack-8329177.webp?v=1769246875";
    private static final String PRB01_BOX_IMAGE = "https://remicardtrader.ca/cdn/shop/files/IMG_OnePieceCG_BoxPRB01.png?v=1776701571&width=720";
    private static final String BONANZA_PACK_IMAGE = "https://www.cherrycollectables.com.au/cdn/shop/files/KON18812--Yu-Gi-Oh-Quarter-Century-Bonanza-Booster-24ct-CDU-01_1024x1024.png?v=1722851996";
    private static final String FOUNDATIONS_PACK_IMAGE = "https://www.theswordandboardtoronto.com/cdn/shop/files/SEA-PAC-MTG-D36300000_745x1040ratio__91628_800x800_crop_center%402x.jpg?v=1730499762";
    private static final String MODERN_HORIZONS_BOX_IMAGE = "https://store.401games.ca/cdn/shop/files/MTGMH3_EN_BstrDspBx_Play_01_03_grande.png?v=1708798262";
    private static final String DECK_BOX_IMAGE = "https://www.pokebeach.com/news/2025/01/deckcase_kissa.jpg";
    private static final String PLAYMAT_IMAGE = "https://chronicsportscards.com/cdn/shop/files/11995110185071648306_2048.jpg?v=1743456802&width=1946";
    private static final String MAGNETIC_HOLDER_IMAGE = "https://displayzoneshop.com/cdn/shop/files/35pt_magnetic_card_holder_display_zone.jpg?v=1743775762&width=1946";
    private static final String COLLECTOR_BINDER_IMAGE = "https://wearecardguardian.com/cdn/shop/files/2_539d3bec-898d-48c6-b767-582f41068290.png?v=1719572619";
    private static final String COMMANDER_CASE_IMAGE = "https://mtgonslaught.com/cdn/shop/files/5_set_filled.png?v=1747865332&width=1946";

    private static final Map<String, String> PRODUCT_IMAGE_URLS = createProductImageUrls();

    private final ProductRepository productRepository;
    private final CardGameRepository cardGameRepository;
    private final MarketplaceStoreRepository marketplaceStoreRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalCatalogSeeder(
            ProductRepository productRepository,
            CardGameRepository cardGameRepository,
            MarketplaceStoreRepository marketplaceStoreRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.productRepository = productRepository;
        this.cardGameRepository = cardGameRepository;
        this.marketplaceStoreRepository = marketplaceStoreRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, CardGame> games = seedGames();
        User demoSeller = getOrCreateDemoSeller();
        Map<String, MarketplaceStore> stores = seedStores(demoSeller.getUaId());

        for (SeedProduct seed : sampleProducts()) {
            if (productRepository.findByProSku(seed.sku()).isPresent()) {
                continue;
            }

            Product product = new Product();
            product.setProSku(seed.sku());
            product.setProName(seed.name());
            product.setGameId(games.get(seed.game()).getGameId());
            product.setProType(seed.type());
            product.setProCost(seed.cost());
            product.setProPriceOfSell(seed.price());
            product.setProQuantity(seed.stock());
            product.setProImageUrl(imageUrlFor(seed.game(), seed.type(), seed.name()));
            product.setProDescription(seed.description());
            product.setProAttributes("{\"condition\":\"Near Mint\",\"seeded\":true}");
            product.setIsActive(true);
            product.setListingSource(seed.source());
            product.setStore(MARKETPLACE.equals(seed.source()) ? stores.get(seed.storeSlug()) : null);
            productRepository.save(product);
        }

        // Existing local rows may predate the catalog seeder. Refresh every
        // row from the product-name map so old generic demo URLs are replaced
        // by the image that matches its pro_name.
        updateProductImages();
    }

    private void updateProductImages() {
        for (Product product : productRepository.findAll()) {
            String game = product.getCardGame() == null ? null : product.getCardGame().getGameName();
            String imageUrl = imageUrlFor(game, product.getProType(), product.getProName());
            if (imageUrl != null && !imageUrl.equals(product.getProImageUrl())) {
                product.setProImageUrl(imageUrl);
                productRepository.save(product);
            }
        }
    }

    private String imageUrlFor(String game, String type, String name) {
        String productName = name == null ? "" : name.trim();
        String productImage = PRODUCT_IMAGE_URLS.get(productName);
        if (productImage != null) {
            return productImage;
        }

        String normalizedType = type == null ? "" : type.toLowerCase();
        String normalizedName = name == null ? "" : name.toLowerCase();

        if (normalizedType.contains("access") || normalizedName.contains("sleeve")
                || normalizedName.contains("binder") || normalizedName.contains("holder")
                || normalizedName.contains("playmat") || normalizedName.contains("deck box")) {
            return normalizedName.contains("binder") ? BINDER_IMAGE : SLEEVES_IMAGE;
        }

        if (game == null) {
            return POKEMON_PACK_IMAGE;
        }

        if (normalizedType.contains("booster box") || normalizedType.equals("box")) {
            return switch (game) {
                case "Pokemon" -> POKEMON_BOX_IMAGE;
                case "One Piece" -> ONE_PIECE_PACK_IMAGE;
                case "Yu-Gi-Oh!" -> YUGIOH_BOX_IMAGE;
                case "Magic: The Gathering" -> MAGIC_IMAGE;
                default -> POKEMON_BOX_IMAGE;
            };
        }

        if (normalizedType.contains("booster") || normalizedType.contains("pack")) {
            return switch (game) {
                case "Pokemon" -> POKEMON_PACK_IMAGE;
                case "One Piece" -> ONE_PIECE_PACK_IMAGE;
                case "Yu-Gi-Oh!" -> YUGIOH_BOX_IMAGE;
                case "Magic: The Gathering" -> MAGIC_IMAGE;
                default -> POKEMON_PACK_IMAGE;
            };
        }

        return switch (game) {
            case "Pokemon" -> POKEMON_CARD_IMAGE;
            case "One Piece" -> ONE_PIECE_CARD_IMAGE;
            case "Yu-Gi-Oh!" -> YUGIOH_CARD_IMAGE;
            case "Magic: The Gathering" -> MAGIC_IMAGE;
            default -> POKEMON_CARD_IMAGE;
        };
    }

    private static Map<String, String> createProductImageUrls() {
        Map<String, String> images = new HashMap<>();

        // Official singles.
        images.put("Pikachu ex Special Illustration", PIKACHU_EX_IMAGE);
        images.put("Monkey D. Luffy Leader Parallel", LUFFY_LEADER_IMAGE);
        images.put("Dark Magician 25th Anniversary", DARK_MAGICIAN_IMAGE);
        images.put("Liliana of the Veil Borderless", LILIANA_IMAGE);

        // Official sealed products.
        images.put("Pokémon Journey Together Booster Pack", JOURNEY_PACK_IMAGE);
        images.put("One Piece OP-10 Royal Blood Booster", OP10_PACK_IMAGE);
        images.put("Yu-Gi-Oh! Alliance Insight Booster", ALLIANCE_PACK_IMAGE);
        images.put("MTG Aetherdrift Play Booster", AETHERDRIFT_PACK_IMAGE);
        images.put("Pokémon Journey Together Booster Box", POKEMON_BOX_IMAGE);
        images.put("One Piece OP-10 Booster Box", OP10_BOX_IMAGE);
        images.put("Yu-Gi-Oh! Alliance Insight Box", ALLIANCE_BOX_IMAGE);
        images.put("MTG Aetherdrift Play Booster Box", AETHERDRIFT_BOX_IMAGE);

        // Official accessories.
        images.put("Optracard Matte Sleeves - Blue", SLEEVES_IMAGE);
        images.put("Premium Zip Binder 12-Pocket", COLLECTOR_BINDER_IMAGE);
        images.put("Magnetic Card Holder 35pt", MAGNETIC_HOLDER_IMAGE);
        images.put("TCG Neoprene Playmat - Midnight", PLAYMAT_IMAGE);

        // Marketplace singles and sealed products.
        images.put("Gengar VMAX Alternate Art", GENGAR_IMAGE);
        images.put("Nami Manga Rare", NAMI_IMAGE);
        images.put("Blue-Eyes White Dragon Ghost Rare", BLUE_EYES_IMAGE);
        images.put("Mana Crypt Borderless", MANA_CRYPT_IMAGE);
        images.put("Pokémon 151 Korean Booster Pack", POKEMON_151_KOREAN_IMAGE);
        images.put("One Piece PRB-01 The Best Booster", PRB01_PACK_IMAGE);
        images.put("Yu-Gi-Oh! Quarter Century Bonanza Pack", BONANZA_PACK_IMAGE);
        images.put("MTG Foundations Collector Booster", FOUNDATIONS_PACK_IMAGE);
        images.put("Pokémon 151 Japanese Booster Box", "https://pullsandpourslgs.com/cdn/shop/files/612gtA-X60L._AC_UF894_1000_QL80.jpg?v=1736180053");
        images.put("One Piece PRB-01 Booster Box", PRB01_BOX_IMAGE);
        images.put("Yu-Gi-Oh! Rarity Collection Box", YUGIOH_BOX_IMAGE);
        images.put("MTG Modern Horizons 3 Play Box", MODERN_HORIZONS_BOX_IMAGE);

        // Marketplace accessories.
        images.put("Used Pokémon Center Deck Box", DECK_BOX_IMAGE);
        images.put("One Piece Straw Hat Playmat", PLAYMAT_IMAGE);
        images.put("Yu-Gi-Oh! 9-Pocket Collector Binder", COLLECTOR_BINDER_IMAGE);
        images.put("MTG Commander Deck Case", COMMANDER_CASE_IMAGE);

        // Rows that were already present before the local catalog seed was added.
        images.put("Monkey D. Luffy OP-16", "https://playvault.ae/cdn/shop/files/OP16japaneseboosterbox.jpg?v=1777886352");
        images.put("Raichu & Alolan Raichu GX", POKEMON_CARD_IMAGE);
        images.put("พี่หน่วง พิธีกรผมสวย", "https://f.ptcdn.info/444/089/000/mgj0k0e4og31uFZukTQ-o.jpg");

        return images;
    }

    private Map<String, CardGame> seedGames() {
        Map<String, CardGame> games = new HashMap<>();
        games.put("Pokemon", getOrCreateGame("Pokemon", "Pokémon Trading Card Game"));
        games.put("One Piece", getOrCreateGame("One Piece", "One Piece Card Game"));
        games.put("Magic: The Gathering", getOrCreateGame("Magic: The Gathering", "Magic: The Gathering"));
        games.put("Yu-Gi-Oh!", getOrCreateGame("Yu-Gi-Oh!", "Yu-Gi-Oh! Trading Card Game"));
        return games;
    }

    private CardGame getOrCreateGame(String name, String description) {
        return cardGameRepository.findByGameName(name).orElseGet(() -> {
            CardGame game = new CardGame();
            game.setGameName(name);
            game.setGameDescription(description);
            return cardGameRepository.save(game);
        });
    }

    private User getOrCreateDemoSeller() {
        return userRepository.findByEmail("marketplace.demo@optracard.local").orElseGet(() -> {
            User user = new User();
            user.setUsername("marketplace_demo_seller");
            user.setEmail("marketplace.demo@optracard.local");
            user.setPassword(passwordEncoder.encode("local-demo-only"));
            user.setRole("USER");
            user.setPhone("0800000000");
            user.setAddress("Local catalog seed data");
            return userRepository.save(user);
        });
    }

    private Map<String, MarketplaceStore> seedStores(Integer sellerUserId) {
        Map<String, MarketplaceStore> stores = new HashMap<>();
        stores.put("card-corner-bkk", getOrCreateStore(sellerUserId, "Card Corner BKK", "card-corner-bkk", "Pokémon and One Piece singles"));
        stores.put("moonlight-tcg", getOrCreateStore(sellerUserId, "Moonlight TCG", "moonlight-tcg", "Collector cards and sealed products"));
        stores.put("shuffle-house", getOrCreateStore(sellerUserId, "Shuffle House", "shuffle-house", "Trading cards and tabletop accessories"));
        stores.put("rare-finds", getOrCreateStore(sellerUserId, "Rare Finds TCG", "rare-finds", "Curated cards for serious collectors"));
        return stores;
    }

    private MarketplaceStore getOrCreateStore(Integer sellerUserId, String name, String slug, String description) {
        return marketplaceStoreRepository.findByStoreSlug(slug).orElseGet(() -> {
            MarketplaceStore store = new MarketplaceStore();
            store.setSellerUserId(sellerUserId);
            store.setStoreName(name);
            store.setStoreSlug(slug);
            store.setStoreStatus("APPROVED");
            store.setStoreDescription(description);
            return marketplaceStoreRepository.save(store);
        });
    }

    private List<SeedProduct> sampleProducts() {
        return List.of(
                // Optracard Official Store: 4 product types x 4 products.
                seed("OFF-SIN-001", "Pikachu ex Special Illustration", "Single", "Pokemon", 760, 990, 6, OFFICIAL, null),
                seed("OFF-SIN-002", "Monkey D. Luffy Leader Parallel", "Single", "One Piece", 1_450, 1_890, 4, OFFICIAL, null),
                seed("OFF-SIN-003", "Dark Magician 25th Anniversary", "Single", "Yu-Gi-Oh!", 1_700, 2_190, 3, OFFICIAL, null),
                seed("OFF-SIN-004", "Liliana of the Veil Borderless", "Single", "Magic: The Gathering", 1_050, 1_390, 5, OFFICIAL, null),
                seed("OFF-BST-001", "Pokémon Journey Together Booster Pack", "Booster", "Pokemon", 118, 149, 36, OFFICIAL, null),
                seed("OFF-BST-002", "One Piece OP-10 Royal Blood Booster", "Booster", "One Piece", 125, 159, 28, OFFICIAL, null),
                seed("OFF-BST-003", "Yu-Gi-Oh! Alliance Insight Booster", "Booster", "Yu-Gi-Oh!", 105, 139, 40, OFFICIAL, null),
                seed("OFF-BST-004", "MTG Aetherdrift Play Booster", "Booster", "Magic: The Gathering", 145, 189, 24, OFFICIAL, null),
                seed("OFF-BOX-001", "Pokémon Journey Together Booster Box", "Booster Box", "Pokemon", 3_780, 4_590, 8, OFFICIAL, null),
                seed("OFF-BOX-002", "One Piece OP-10 Booster Box", "Booster Box", "One Piece", 2_750, 3_390, 10, OFFICIAL, null),
                seed("OFF-BOX-003", "Yu-Gi-Oh! Alliance Insight Box", "Booster Box", "Yu-Gi-Oh!", 2_090, 2_590, 7, OFFICIAL, null),
                seed("OFF-BOX-004", "MTG Aetherdrift Play Booster Box", "Booster Box", "Magic: The Gathering", 4_250, 5_190, 6, OFFICIAL, null),
                seed("OFF-ACC-001", "Optracard Matte Sleeves - Blue", "Accessories", "Pokemon", 170, 220, 48, OFFICIAL, null),
                seed("OFF-ACC-002", "Premium Zip Binder 12-Pocket", "Accessories", "One Piece", 920, 1_190, 12, OFFICIAL, null),
                seed("OFF-ACC-003", "Magnetic Card Holder 35pt", "Accessories", "Yu-Gi-Oh!", 115, 159, 60, OFFICIAL, null),
                seed("OFF-ACC-004", "TCG Neoprene Playmat - Midnight", "Accessories", "Magic: The Gathering", 620, 790, 18, OFFICIAL, null),

                // Approved seller listings: the same 4 product types x 4 products.
                seed("MKT-SIN-001", "Gengar VMAX Alternate Art", "Single", "Pokemon", 4_800, 5_450, 1, MARKETPLACE, "card-corner-bkk"),
                seed("MKT-SIN-002", "Nami Manga Rare", "Single", "One Piece", 19_500, 22_900, 1, MARKETPLACE, "moonlight-tcg"),
                seed("MKT-SIN-003", "Blue-Eyes White Dragon Ghost Rare", "Single", "Yu-Gi-Oh!", 8_400, 9_900, 2, MARKETPLACE, "rare-finds"),
                seed("MKT-SIN-004", "Mana Crypt Borderless", "Single", "Magic: The Gathering", 5_100, 5_950, 1, MARKETPLACE, "shuffle-house"),
                seed("MKT-BST-001", "Pokémon 151 Korean Booster Pack", "Booster", "Pokemon", 78, 109, 30, MARKETPLACE, "card-corner-bkk"),
                seed("MKT-BST-002", "One Piece PRB-01 The Best Booster", "Booster", "One Piece", 135, 175, 22, MARKETPLACE, "moonlight-tcg"),
                seed("MKT-BST-003", "Yu-Gi-Oh! Quarter Century Bonanza Pack", "Booster", "Yu-Gi-Oh!", 165, 210, 16, MARKETPLACE, "rare-finds"),
                seed("MKT-BST-004", "MTG Foundations Collector Booster", "Booster", "Magic: The Gathering", 690, 820, 9, MARKETPLACE, "shuffle-house"),
                seed("MKT-BOX-001", "Pokémon 151 Japanese Booster Box", "Booster Box", "Pokemon", 4_900, 5_650, 4, MARKETPLACE, "card-corner-bkk"),
                seed("MKT-BOX-002", "One Piece PRB-01 Booster Box", "Booster Box", "One Piece", 3_250, 3_890, 5, MARKETPLACE, "moonlight-tcg"),
                seed("MKT-BOX-003", "Yu-Gi-Oh! Rarity Collection Box", "Booster Box", "Yu-Gi-Oh!", 2_450, 2_950, 6, MARKETPLACE, "rare-finds"),
                seed("MKT-BOX-004", "MTG Modern Horizons 3 Play Box", "Booster Box", "Magic: The Gathering", 6_800, 7_790, 3, MARKETPLACE, "shuffle-house"),
                seed("MKT-ACC-001", "Used Pokémon Center Deck Box", "Accessories", "Pokemon", 210, 290, 7, MARKETPLACE, "card-corner-bkk"),
                seed("MKT-ACC-002", "One Piece Straw Hat Playmat", "Accessories", "One Piece", 580, 720, 5, MARKETPLACE, "moonlight-tcg"),
                seed("MKT-ACC-003", "Yu-Gi-Oh! 9-Pocket Collector Binder", "Accessories", "Yu-Gi-Oh!", 690, 850, 4, MARKETPLACE, "rare-finds"),
                seed("MKT-ACC-004", "MTG Commander Deck Case", "Accessories", "Magic: The Gathering", 380, 490, 11, MARKETPLACE, "shuffle-house")
        );
    }

    private SeedProduct seed(
            String sku,
            String name,
            String type,
            String game,
            double cost,
            double price,
            int stock,
            String source,
            String storeSlug
    ) {
        return new SeedProduct(
                sku,
                name,
                type,
                game,
                cost,
                price,
                stock,
                source,
                storeSlug,
                "Sample local catalog listing for " + name + "."
        );
    }

    private record SeedProduct(
            String sku,
            String name,
            String type,
            String game,
            double cost,
            double price,
            int stock,
            String source,
            String storeSlug,
            String description
    ) {}
}
