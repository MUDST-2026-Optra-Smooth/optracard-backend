package org.example.service;

import org.example.model.Cart;
import org.example.model.CartItem;
import org.example.model.Product;
import org.example.repository.CartRepository;
import org.example.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductDeletionServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CartRepository cartRepository;

    @Test
    void permanentlyDelete_removesMatchingCartItemsBeforeDeletingTheProduct() {
        Product product = new Product();
        product.setProId(42);

        Cart cartWithProduct = cartWithProduct(42);
        Cart cartWithAnotherProduct = cartWithProduct(7);
        when(cartRepository.findAll()).thenReturn(List.of(cartWithProduct, cartWithAnotherProduct));

        new ProductDeletionService(productRepository, cartRepository).permanentlyDelete(product);

        assertEquals(0, cartWithProduct.getItems().size());
        assertEquals(1, cartWithAnotherProduct.getItems().size());
        verify(cartRepository).save(cartWithProduct);
        verify(productRepository).delete(product);
        verify(productRepository).flush();
    }

    private Cart cartWithProduct(int productId) {
        Cart cart = new Cart();
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setCart(cart);
        cart.getItems().add(item);
        return cart;
    }
}
