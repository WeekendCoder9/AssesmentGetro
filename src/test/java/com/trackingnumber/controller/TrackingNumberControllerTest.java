package com.trackingnumber.controller;

import com.trackingnumber.domain.TrackingNumberRequest;
import com.trackingnumber.exception.TrackingNumberException;
import com.trackingnumber.service.TrackingNumberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(TrackingNumberController.class)
class TrackingNumberControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private TrackingNumberService trackingNumberService;
    
    @Test
    void shouldGenerateTrackingNumber() {
        String expectedTrackingNumber = "ABC123DEF4";
        TrackingNumberRequest request = new TrackingNumberRequest(
            "US", "CA", "1.234", "2018-11-20T19:29:32+08:00",
            "de619854-b59b-425e-9db4-943379e1bd49", "RedBox Logistics", "redbox-logistics"
        );
        
        when(trackingNumberService.generateUniqueTrackingNumber(any(TrackingNumberRequest.class)))
            .thenReturn(Mono.just(expectedTrackingNumber));
        
        webTestClient.post()
            .uri("/api/v1/next-tracking-number")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.tracking_number").isEqualTo(expectedTrackingNumber)
            .jsonPath("$.created_at").exists();
    }
    
    @Test
    void shouldReturnBadRequestForInvalidRequest() {
        TrackingNumberRequest invalidRequest = new TrackingNumberRequest(
            "", "CA", "1.234", "2018-11-20T19:29:32+08:00",
            "de619854-b59b-425e-9db4-943379e1bd49", "RedBox Logistics", "redbox-logistics"
        );
        
        webTestClient.post()
            .uri("/api/v1/next-tracking-number")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.error").exists()
            .jsonPath("$.details").exists();
    }
    
    @Test
    void shouldHandleServiceError() {
        TrackingNumberRequest request = new TrackingNumberRequest(
            "US", "CA", "1.234", "2018-11-20T19:29:32+08:00",
            "de619854-b59b-425e-9db4-943379e1bd49", "RedBox Logistics", "redbox-logistics"
        );
        
        when(trackingNumberService.generateUniqueTrackingNumber(any(TrackingNumberRequest.class)))
            .thenReturn(Mono.error(new TrackingNumberException("Generation failed")));
        
        webTestClient.post()
            .uri("/api/v1/next-tracking-number")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError()
            .expectBody()
            .jsonPath("$.error").exists()
            .jsonPath("$.status").isEqualTo(500);
    }
    
    @Test
    void shouldReturnHealthStatus() {
        webTestClient.get()
            .uri("/api/v1/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .isEqualTo("OK");
    }
}
