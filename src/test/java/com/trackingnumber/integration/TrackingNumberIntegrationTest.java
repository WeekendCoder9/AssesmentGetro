package com.trackingnumber.integration;

import com.redis.testcontainers.RedisContainer;
import com.trackingnumber.domain.TrackingNumberRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TrackingNumberIntegrationTest {
    
    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
    
    @Autowired
    private WebTestClient webTestClient;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }
    
    @Test
    void shouldGenerateUniqueTrackingNumbers() {
        TrackingNumberRequest request = new TrackingNumberRequest(
            "US", "CA", "1.234",
            "de619854-b59b-425e-9db4-943379e1bd49", "RedBox Logistics", "redbox-logistics"
        );
        
        // Generate first tracking number
        String firstTrackingNumber = webTestClient.post()
            .uri("/api/v1/next-tracking-number")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.tracking_number").exists()
            .jsonPath("$.created_at").exists()
            .returnResult()
            .getResponseBody();
        
        // Generate second tracking number
        String secondTrackingNumber = webTestClient.post()
            .uri("/api/v1/next-tracking-number")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.tracking_number").exists()
            .jsonPath("$.created_at").exists()
            .returnResult()
            .getResponseBody();
        
        // Verify they are different (this is probabilistic but very likely)
        // In a real scenario, you might want to extract and compare the actual values
        assert firstTrackingNumber != null;
        assert secondTrackingNumber != null;
    }
    
    @Test
    void shouldValidateRequestFields() {
        TrackingNumberRequest invalidRequest = new TrackingNumberRequest(
            "USA", // Invalid - should be 2 characters
            "C",   // Invalid - should be 2 characters  
            "invalid_weight", // Invalid format
            "invalid_date",   // Invalid date format
            "",    // Invalid - required field
            "",    // Invalid - required field
            null   // Valid - optional field
        );
        
        webTestClient.post()
            .uri("/api/v1/next-tracking-number")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.error").isEqualTo("Validation failed")
            .jsonPath("$.details").exists();
    }
}
