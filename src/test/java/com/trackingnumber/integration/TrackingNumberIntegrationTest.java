package com.trackingnumber.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TrackingNumberIntegrationTest {
    
    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
    
    @Autowired
    private WebTestClient webTestClient;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }
    
    @Test
    void shouldGenerateUniqueTrackingNumbers() throws Exception {
        TrackingNumberRequest request = new TrackingNumberRequest(
            "US", "CA", "1.234",
            "de619854-b59b-425e-9db4-943379e1bd49", "RedBox Logistics", "redbox-logistics"
        );
        
        // Generate first tracking number
        byte[] firstResponse = webTestClient.post()
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
        byte[] secondResponse = webTestClient.post()
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
        
        // Parse responses and compare tracking numbers
        JsonNode firstJson = objectMapper.readTree(firstResponse);
        JsonNode secondJson = objectMapper.readTree(secondResponse);
        
        String firstTrackingNumber = firstJson.get("tracking_number").asText();
        String secondTrackingNumber = secondJson.get("tracking_number").asText();
        
        // Verify they are different
        assertNotEquals(firstTrackingNumber, secondTrackingNumber);
        assertNotNull(firstTrackingNumber);
        assertNotNull(secondTrackingNumber);
        assertTrue(firstTrackingNumber.matches("^[A-Z0-9]{1,16}$"));
        assertTrue(secondTrackingNumber.matches("^[A-Z0-9]{1,16}$"));
    }
    
    @Test
    void shouldValidateRequestFields() {
        TrackingNumberRequest invalidRequest = new TrackingNumberRequest(
            "USA", // Invalid - should be 2 characters
            "C",   // Invalid - should be 2 characters  
            "invalid_weight", // Invalid format
            "de619854-b59b-425e-9db4-943379e1bd49",
            "",    // Invalid - required field (customer name)
            "valid-slug"
        );
        
        webTestClient.post()
            .uri("/api/v1/next-tracking-number")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.error").isEqualTo("Validation failed")
            .jsonPath("$.details").exists()
            .jsonPath("$.details.originCountryId").exists()
            .jsonPath("$.details.destinationCountryId").exists()
            .jsonPath("$.details.weight").exists()
            .jsonPath("$.details.customerName").exists();
    }
    
    @Test
    void shouldHandleEmptyRequestBody() {
        webTestClient.post()
            .uri("/api/v1/next-tracking-number")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.error").isEqualTo("Validation failed")
            .jsonPath("$.details").exists();
    }
    
    @Test
    void shouldAcceptValidRequest() {
        TrackingNumberRequest validRequest = new TrackingNumberRequest(
            "US", "CA", "1.234",
            "de619854-b59b-425e-9db4-943379e1bd49", 
            "RedBox Logistics", 
            "redbox-logistics"
        );
        
        webTestClient.post()
            .uri("/api/v1/next-tracking-number")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.tracking_number").exists()
            .jsonPath("$.created_at").exists();
    }
}
