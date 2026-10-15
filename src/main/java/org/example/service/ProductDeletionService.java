package org.example.service;

import org.example.model.Cart;
import org.example.model.Product;
import org.example.repository.CartRepository;
import org.example.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Removes a product record permanently while keeping shoppers' carts valid.
 * Orders deliberately keep their own immutable price/quantity snapshots.
 */
@Service
public class ProductDeletionService {
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;

    public ProductDeletionService(ProductRepository productRepository, CartRepository cartRepository) {
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
    }

    @Transactional
    public void permanentlyDelete(Product product) {
        Integer productId = product.getProId();
        for (Cart cart : cartRepository.findAll()) {
            boolean removed = cart.getItems().removeIf(item -> productId.equals(item.getProductId()));
            if (removed) {
                // orphanRemoval removes the matching CartItems row as part of this transaction.
                cartRepository.save(cart);
            }
        }
        productRepository.delete(product);
        productRepository.flush();
    }
}
