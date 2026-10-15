package org.example.controller;

import org.example.dto.SellerProductResponse;
import org.example.model.Product;
import org.example.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMarketplaceControllerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminMarketplaceController controller;

    private Product marketplaceProduct;

    @BeforeEach
    void setUp() {
        marketplaceProduct = new Product();
        marketplaceProduct.setProId(10);
        marketplaceProduct.setProName("Charizard VMAX");
        marketplaceProduct.setListingSource("MARKETPLACE");
        marketplaceProduct.setApprovalStatus("PENDING");
        marketplaceProduct.setIsActive(false);
    }

    private void mockAdmin() {
        doReturn((Collection) List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();
    }

    @Test
    void list_whenAdmin_returnsFilteredProducts() {
        mockAdmin();
        when(productRepository.findByListingSourceOrderByProIdDesc("MARKETPLACE"))
                .thenReturn(List.of(marketplaceProduct));

        List<SellerProductResponse> responses = controller.list(authentication, "PENDING");

        assertEquals(1, responses.size());
        assertEquals("Charizard VMAX", responses.get(0).name());
        assertEquals("PENDING", responses.get(0).approvalStatus());
    }

    @Test
    void list_whenNotAdmin_throwsForbidden() {
        doReturn((Collection) List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .when(authentication).getAuthorities();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.list(authentication, "ALL"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void approve_approvesAndActivatesProduct() {
        mockAdmin();
        when(productRepository.findByProId(10)).thenReturn(Optional.of(marketplaceProduct));
        when(productRepository.saveAndFlush(marketplaceProduct)).thenReturn(marketplaceProduct);

        SellerProductResponse response = controller.approve(authentication, 10);

        assertEquals("APPROVED", response.approvalStatus());
        assertTrue(response.active());
        verify(productRepository).saveAndFlush(marketplaceProduct);
    }

    @Test
    void reject_rejectsAndDeactivatesProduct() {
        mockAdmin();
        when(productRepository.findByProId(10)).thenReturn(Optional.of(marketplaceProduct));
        when(productRepository.saveAndFlush(marketplaceProduct)).thenReturn(marketplaceProduct);

        SellerProductResponse response = controller.reject(authentication, 10, new AdminMarketplaceController.ReviewRequest("Rejected reason"));

        assertEquals("REJECTED", response.approvalStatus());
        assertFalse(response.active());
        verify(productRepository).saveAndFlush(marketplaceProduct);
    }
}
