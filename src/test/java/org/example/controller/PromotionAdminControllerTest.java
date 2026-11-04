package org.example.controller;

import org.example.dto.PromotionRequest;
import org.example.dto.PromotionResponse;
import org.example.service.PromotionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionAdminControllerTest {

    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private PromotionAdminController controller;

    @Test
    @DisplayName("promotions and promotion by ID delegate to service")
    void getPromotions_success() {
        PromotionResponse p1 = mock(PromotionResponse.class);
        when(promotionService.getPromotions()).thenReturn(List.of(p1));
        when(promotionService.getPromotion(10)).thenReturn(p1);

        assertEquals(1, controller.promotions().size());
        assertSame(p1, controller.promotion(10));
    }

    @Test
    @DisplayName("create returns 201 Created with created promotion")
    void create_success() {
        PromotionRequest req = mock(PromotionRequest.class);
        PromotionResponse resp = mock(PromotionResponse.class);
        when(promotionService.createPromotion(req)).thenReturn(resp);

        ResponseEntity<PromotionResponse> result = controller.create(req);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertSame(resp, result.getBody());
        verify(promotionService).createPromotion(req);
    }

    @Test
    @DisplayName("update returns updated promotion response")
    void update_success() {
        PromotionRequest req = mock(PromotionRequest.class);
        PromotionResponse resp = mock(PromotionResponse.class);
        when(promotionService.updatePromotion(5, req)).thenReturn(resp);

        PromotionResponse actual = controller.update(5, req);

        assertSame(resp, actual);
        verify(promotionService).updatePromotion(5, req);
    }

    @Test
    @DisplayName("delete returns 204 No Content")
    void delete_success() {
        ResponseEntity<Void> result = controller.delete(12);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(promotionService).deletePromotion(12);
    }
}

