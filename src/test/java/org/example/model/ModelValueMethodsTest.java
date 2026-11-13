package org.example.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the generated value methods on the JPA models.  Lombok's generated
 * equals/hashCode methods contain a null and mismatch branch for every field;
 * keeping those branches covered prevents model changes from silently reducing
 * the application's real coverage.
 */
class ModelValueMethodsTest {

    @Test
    void productValueMethodsCoverEqualNullAndFieldMismatchPaths() throws Exception {
        Product first = populatedProduct();
        Product same = populatedProduct();
        // CardGame and MarketplaceStore do not override equals, so use the
        // same relation instances when comparing otherwise identical products.
        same.setCardGame(first.getCardGame());
        same.setStore(first.getStore());

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, new Object());

        for (Field field : valueFields(Product.class)) {
            Object original = field.get(same);
            Object alternate = alternateValue(field, original);
            if (alternate != NO_ALTERNATE) {
                field.set(same, alternate);
                assertNotEquals(first, same, "field should participate in equality: " + field.getName());
                field.set(same, original);
            }
        }

        Product allNull = new Product();
        for (Field field : valueFields(Product.class)) {
            field.set(allNull, null);
        }
        Product anotherNull = new Product();
        for (Field field : valueFields(Product.class)) {
            field.set(anotherNull, null);
        }
        assertEquals(allNull, anotherNull);
        assertEquals(allNull.hashCode(), anotherNull.hashCode());
    }

    @Test
    void userValueMethodsCoverEqualNullAndFieldMismatchPaths() throws Exception {
        User first = populatedUser();
        User same = populatedUser();

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, new Object());

        for (Field field : valueFields(User.class)) {
            Object original = field.get(same);
            Object alternate = alternateValue(field, original);
            if (alternate != NO_ALTERNATE) {
                field.set(same, alternate);
                assertNotEquals(first, same, "field should participate in equality: " + field.getName());
                field.set(same, original);
            }
        }

        User allNull = new User();
        User anotherNull = new User();
        for (Field field : valueFields(User.class)) {
            field.set(allNull, null);
            field.set(anotherNull, null);
        }
        assertEquals(allNull, anotherNull);
        assertEquals(allNull.hashCode(), anotherNull.hashCode());
    }

    private static Product populatedProduct() {
        CardGame game = new CardGame();
        game.setGameId(7);
        game.setGameName("Pokemon");
        MarketplaceStore store = new MarketplaceStore();
        store.setStoreId(9);

        Product product = new Product();
        product.setAuctionReserved(true);
        product.setProId(1);
        product.setGameId(7);
        product.setCardGame(game);
        product.setProName("Pikachu");
        product.setProCost(10.5);
        product.setProPriceOfSell(20.5);
        product.setProQuantity(3);
        product.setProType("SINGLE");
        product.setProImageUrl("https://example.test/card.png");
        product.setProductSet("Base");
        product.setLanguage("EN");
        product.setProDescription("A card");
        product.setIsActive(true);
        product.setListingSource("OFFICIAL");
        product.setApprovalStatus("APPROVED");
        product.setStore(store);
        product.setTemplateProductId(4);
        return product;
    }

    private static User populatedUser() {
        User user = new User();
        user.setUaId(1);
        user.setUsername("tester");
        user.setPassword("secret");
        user.setRole("USER");
        user.setEmail("tester@example.test");
        user.setPhone("0123456789");
        user.setAddress("Somewhere");
        user.setProfileImage("https://example.test/avatar.png");
        user.setCreateDate(LocalDateTime.of(2026, 1, 1, 12, 0));
        return user;
    }

    private static Field[] valueFields(Class<?> type) {
        return java.util.Arrays.stream(type.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()) && !field.isSynthetic())
                .peek(field -> field.setAccessible(true))
                .toArray(Field[]::new);
    }

    private static final Object NO_ALTERNATE = new Object();

    private static Object alternateValue(Field field, Object value) {
        Class<?> type = field.getType();
        if (type == String.class) return value == null ? "changed" : value + "-changed";
        if (type == Integer.class || type == int.class) return value == null ? 1 : ((Integer) value) + 1;
        if (type == Double.class || type == double.class) return value == null ? 1.0 : ((Double) value) + 1.0;
        if (type == Boolean.class || type == boolean.class) return value == null || !((Boolean) value);
        if (type == LocalDateTime.class) return value == null ? LocalDateTime.of(2026, 1, 1, 0, 0) : ((LocalDateTime) value).plusSeconds(1);
        if (type == CardGame.class) {
            CardGame game = new CardGame();
            game.setGameId(99);
            return game;
        }
        if (type == MarketplaceStore.class) {
            MarketplaceStore store = new MarketplaceStore();
            store.setStoreId(99);
            return store;
        }
        return NO_ALTERNATE;
    }
}
