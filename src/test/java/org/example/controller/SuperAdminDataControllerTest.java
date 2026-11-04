package org.example.controller;

import org.example.dto.SuperAdminData;
import org.example.service.SuperAdminDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuperAdminDataControllerTest {

    @Mock
    private SuperAdminDataService service;

    @InjectMocks
    private SuperAdminDataController controller;

    @Test
    @DisplayName("overview delegates to service")
    void overview_success() {
        SuperAdminData.OverviewResponse mockResponse = mock(SuperAdminData.OverviewResponse.class);
        when(service.getOverview()).thenReturn(mockResponse);

        SuperAdminData.OverviewResponse actual = controller.overview();
        assertSame(mockResponse, actual);
        verify(service).getOverview();
    }

    @Test
    @DisplayName("stores and store by ID delegate to service")
    void stores_success() {
        List<SuperAdminData.StoreResponse> list = List.of();
        when(service.getStores()).thenReturn(list);
        assertSame(list, controller.stores());

        SuperAdminData.StoreResponse storeResp = mock(SuperAdminData.StoreResponse.class);
        when(service.getStore(42)).thenReturn(storeResp);
        assertSame(storeResp, controller.store(42));
    }

    @Test
    @DisplayName("catalog, transactions, users, staff delegate to service")
    void lists_success() {
        when(service.getCatalog()).thenReturn(List.of());
        assertEquals(0, controller.catalog().size());

        when(service.getTransactions()).thenReturn(List.of());
        assertEquals(0, controller.transactions().size());

        when(service.getUsers()).thenReturn(List.of());
        assertEquals(0, controller.users().size());

        when(service.getStaff()).thenReturn(List.of());
        assertEquals(0, controller.staff().size());
    }

    @Test
    @DisplayName("deleteUser delegates with caller username and confirmation")
    void deleteUser_success() {
        Authentication auth = new UsernamePasswordAuthenticationToken("superadmin@optracard.com", "pass");
        SuperAdminData.DeleteAccountRequest req = new SuperAdminData.DeleteAccountRequest("DELETE");

        ResponseEntity<Void> response = controller.deleteUser(99, req, auth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service).deleteUser(99, "superadmin@optracard.com", "DELETE");
    }

    @Test
    @DisplayName("deleteStaff delegates with caller username and confirmation")
    void deleteStaff_success() {
        Authentication auth = new UsernamePasswordAuthenticationToken("superadmin@optracard.com", "pass");
        SuperAdminData.DeleteAccountRequest req = new SuperAdminData.DeleteAccountRequest("DELETE");

        ResponseEntity<Void> response = controller.deleteStaff(55, req, auth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service).deleteStaff(55, "superadmin@optracard.com", "DELETE");
    }

    @Test
    @DisplayName("createStaff returns 201 Created with created staff details")
    void createStaff_success() {
        SuperAdminData.CreateStaffRequest req = new SuperAdminData.CreateStaffRequest(
                "staff1", "staff1@optracard.com", "password123", "0812345678", "HQ Bangkok"
        );
        SuperAdminData.StaffResponse staffResponse = mock(SuperAdminData.StaffResponse.class);
        when(service.createStaff(req)).thenReturn(staffResponse);

        ResponseEntity<SuperAdminData.StaffResponse> response = controller.createStaff(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(staffResponse, response.getBody());
        verify(service).createStaff(req);
    }
}
