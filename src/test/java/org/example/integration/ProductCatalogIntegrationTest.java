package org.example.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.CardGame;
import org.example.model.Product;
import org.example.repository.CardGameRepository;
import org.example.repository.ProductRepository;
import org.example.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductCatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CardGameRepository cardGameRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private CardGame pokemonGame;
    private Product charizardProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        cardGameRepository.deleteAll();

        pokemonGame = new CardGame();
        pokemonGame.setGameName("Pokemon");
        pokemonGame.setGameDescription("Pokemon Trading Card Game");
        pokemonGame = cardGameRepository.save(pokemonGame);

        charizardProduct = new Product();
        charizardProduct.setGameId(pokemonGame.getGameId());
        charizardProduct.setProName("Charizard VMAX");
        charizardProduct.setProCost(500.0);
        charizardProduct.setProPriceOfSell(1200.0);
        charizardProduct.setProQuantity(5);
        charizardProduct.setProType("Single");
        charizardProduct.setProductSet("Darkness Ablaze");
        charizardProduct.setIsActive(true);
        charizardProduct.setListingSource("OFFICIAL");
        charizardProduct.setApprovalStatus("APPROVED");
        charizardProduct = productRepository.save(charizardProduct);
    }

    @Test
    @DisplayName("Public user can browse catalog products")
    void getAllProducts_returnsPublicCatalog() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name").value("Charizard VMAX"))
                .andExpect(jsonPath("$[0].price").value(1200.0))
                .andExpect(jsonPath("$[0].game").value("Pokemon"));
    }

    @Test
    @DisplayName("Public user can view product details by ID")
    void getProductById_returnsProductDetails() throws Exception {
        mockMvc.perform(get("/api/products/" + charizardProduct.getProId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(charizardProduct.getProId()))
                .andExpect(jsonPath("$.name").value("Charizard VMAX"))
                .andExpect(jsonPath("$.price").value(1200.0));
    }

    @Test
    @DisplayName("Querying non-existent product ID returns 404 Not Found")
    void getProductById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/products/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Searching products by keyword filters matching results")
    void searchProducts_byKeyword_returnsMatchingProduct() throws Exception {
        mockMvc.perform(get("/api/products/search?q=Charizard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Charizard VMAX"));

        mockMvc.perform(get("/api/products/search?q=NonExistentCard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchProducts_supportsTyposSynonymsAndPublicVisibility() throws Exception {
        Product hidden = new Product();
        hidden.setGameId(pokemonGame.getGameId());
        hidden.setProName("Hidden Pokemon");
        hidden.setIsActive(true);
        hidden.setListingSource("MARKETPLACE");
        hidden.setApprovalStatus("PENDING");
        productRepository.save(hidden);

        for (String query : new String[]{"pokamon", "โปเกมอน", "โปเกมอน charizard"}) {
            mockMvc.perform(get("/api/products/search").param("q", query))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Charizard VMAX"));
        }
        CardGame onePiece = new CardGame();
        onePiece.setGameName("One Piece");
        onePiece = cardGameRepository.save(onePiece);
        Product luffy = new Product();
        luffy.setGameId(onePiece.getGameId());
        luffy.setProName("Luffy");
        luffy.setIsActive(true);
        productRepository.save(luffy);
        mockMvc.perform(get("/api/products/search").param("q", "วันพีซ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Luffy"));
    }

    @Test
    @DisplayName("Unauthenticated request to create product is rejected with 403 Forbidden")
    void createProduct_unauthenticated_returnsForbiddenOrUnauthorized() throws Exception {
        Product newProduct = new Product();
        newProduct.setProName("Pikachu EX");
        newProduct.setGameId(pokemonGame.getGameId());
        newProduct.setProCost(100.0);
        newProduct.setProPriceOfSell(250.0);
        newProduct.setProQuantity(20);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin user with valid JWT can create a new official product")
    void createProduct_asAdmin_createsAndReturnsProduct() throws Exception {
        String adminToken = jwtUtil.generateToken("admin@optracard.com", "ADMIN");

        Product newProduct = new Product();
        newProduct.setProName("Pikachu EX");
        newProduct.setGameId(pokemonGame.getGameId());
        newProduct.setProCost(100.0);
        newProduct.setProPriceOfSell(250.0);
        newProduct.setProQuantity(20);
        newProduct.setProType("Single");
        newProduct.setListingSource("OFFICIAL");
        newProduct.setIsActive(true);

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proName").value("Pikachu EX"))
                .andExpect(jsonPath("$.proPriceOfSell").value(250.0));

        assertEquals(2, productRepository.count());
    }
}
