package org.example.controller;

import org.example.dto.AdminData;
import org.example.service.AdminDataService;
import org.example.service.MarketplaceStoreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminStoreControllerTest {

    @Mock
    private MarketplaceStoreService storeService;

    @Mock
    private AdminDataService adminDataService;

    @InjectMocks
    private AdminStoreController controller;

    @Test
    @DisplayName("review succeeds when caller is ROLE_ADMIN")
    void review_success_admin() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        AdminStoreController.ReviewRequest request = new AdminStoreController.ReviewRequest("APPROVED", "Verified credentials");
        AdminData.StoreResponse expected = mock(AdminData.StoreResponse.class);

        when(adminDataService.getStore(10)).thenReturn(expected);

        AdminData.StoreResponse actual = controller.review(auth, 10, request);

        assertSame(expected, actual);
        verify(storeService).reviewApplication(10, "APPROVED", "Verified credentials");
        verify(adminDataService).getStore(10);
    }

    @Test
    @DisplayName("review succeeds when caller is ROLE_SUPER_ADMIN")
    void review_success_superAdmin() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "superadmin", "pass", List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        AdminStoreController.ReviewRequest request = new AdminStoreController.ReviewRequest("REJECTED", "Incomplete document");
        AdminData.StoreResponse expected = mock(AdminData.StoreResponse.class);

        when(adminDataService.getStore(5)).thenReturn(expected);

        AdminData.StoreResponse actual = controller.review(auth, 5, request);

        assertSame(expected, actual);
        verify(storeService).reviewApplication(5, "REJECTED", "Incomplete document");
    }

    @Test
    @DisplayName("review throws 403 Forbidden when caller is not an admin")
    void review_forbidden_regularUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        AdminStoreController.ReviewRequest request = new AdminStoreController.ReviewRequest("APPROVED", "note");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.review(auth, 1, request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Administrator access required"));
        verifyNoInteractions(storeService, adminDataService);
    }

    @Test
    @DisplayName("review throws 403 Forbidden when authentication is null")
    void review_forbidden_nullAuth() {
        AdminStoreController.ReviewRequest request = new AdminStoreController.ReviewRequest("APPROVED", "note");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.review(null, 1, request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(storeService, adminDataService);
    }

    @Test
    @DisplayName("review handles null request body safely")
    void review_nullRequestBody() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        AdminData.StoreResponse expected = mock(AdminData.StoreResponse.class);
        when(adminDataService.getStore(2)).thenReturn(expected);

        AdminData.StoreResponse actual = controller.review(auth, 2, null);

        assertSame(expected, actual);
        verify(storeService).reviewApplication(2, null, null);
    }
}
