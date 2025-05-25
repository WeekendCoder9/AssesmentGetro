# Tracking Number Generator

A high-performance, reactive Spring Boot service for generating unique alphanumeric tracking numbers with Redis-backed persistence and collision detection.

## Features

- **Reactive Architecture**: Built with Spring WebFlux for non-blocking operations
- **Guaranteed Uniqueness**: Redis-based atomic operations prevent duplicates
- **Collision Handling**: Automatic retry mechanism with configurable limits
- **Format Validation**: Generates 1-16 character alphanumeric tracking numbers
- **Production Ready**: Comprehensive logging, metrics, health checks, and error handling

## Quick Start

### Prerequisites
- Java 17+
- Redis 6+

### Run with Docker
```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
./mvnw spring-boot:run
```

### Generate Tracking Number
```bash
curl -X POST http://localhost:8080/api/v1/next-tracking-number \
  -H "Content-Type: application/json" \
  -d '{
    "originCountryId": "US",
    "destinationCountryId": "CA",
    "weight": "1.234",
    "customerId": "de619854-b59b-425e-9db4-943379e1bd49",
    "customerName": "RedBox Logistics",
    "customerSlug": "redbox-logistics"
  }'
```

**Response:**
```json
{
  "tracking_number": "ABC123DEF4",
  "created_at": "2025-05-25T10:30:45.123Z"
}
```

## API Reference

### POST `/api/v1/next-tracking-number`
Generates a unique tracking number.

**Request Body:**
| Field | Type | Required | Format | Description |
|-------|------|----------|--------|-------------|
| `originCountryId` | string | ✓ | ISO 3166-1 alpha-2 | Origin country code |
| `destinationCountryId` | string | ✓ | ISO 3166-1 alpha-2 | Destination country code |
| `weight` | string | ✓ | `X.XXX` | Package weight (3 decimal places) |
| `customerId` | string | ✓ | max 36 chars | Customer identifier |
| `customerName` | string | ✓ | max 100 chars | Customer display name |
| `customerSlug` | string | | max 50 chars | Customer URL slug |

### GET `/api/v1/health`
Service health check endpoint.

## Configuration

### Environment Variables
```bash
# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

# Application Settings
TRACKING_NUMBER_MAX_RETRIES=10
TRACKING_NUMBER_TTL_SECONDS=86400

# Logging
LOG_LEVEL_APP=INFO

# Profiles
SPRING_PROFILES_ACTIVE=local
```

### Redis Configuration
The service requires Redis for:
- Atomic uniqueness checks via `SETNX`
- TTL-based cleanup of tracking numbers
- High-performance concurrent access

### Performance Tuning
- **Connection Pool**: Configure `spring.data.redis.lettuce.pool.*`
- **Retry Logic**: Adjust `tracking-number.max-retries`
- **TTL**: Set `tracking-number.ttl-seconds` for cleanup policy

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controller    │───▶│    Service      │───▶│   Generator     │
│   (WebFlux)     │    │  (Uniqueness)   │    │ (Hash-based)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │      Redis      │
                       │ (Atomic Checks) │
                       └─────────────────┘
```

**Key Components:**
- **TrackingNumberController**: REST endpoint handling
- **TrackingNumberServiceImpl**: Business logic and collision detection
- **DefaultTrackingNumberGenerator**: SHA-256 based number generation
- **Redis**: Distributed uniqueness guarantees

## Monitoring

### Health Checks
- **Service**: `GET /actuator/health`
- **Redis**: Automatic connection health monitoring
- **Disk Space**: Configurable threshold monitoring

### Metrics (Prometheus)
- `GET /actuator/prometheus`
- HTTP request metrics with percentiles
- Custom business metrics for generation attempts
- Redis connection pool metrics

### Tracing
- Distributed tracing with configurable sampling
- Span annotations for key operations
- Baggage propagation for customer context

## Testing

```bash
# Unit Tests
./mvnw test

# Integration Tests (requires Docker)
./mvnw test -Dtest=TrackingNumberIntegrationTest

# Performance Tests
./mvnw test -Dtest=TrackingNumberPerformanceTest
```

**Test Coverage:**
- Unit tests for all components
- Integration tests with Testcontainers
- Performance tests for concurrent load
- Validation tests for input constraints

## Production Considerations

### Scalability
- Stateless design enables horizontal scaling
- Redis clustering support for high availability
- Reactive streams handle backpressure automatically

### Reliability
- Graceful shutdown with connection draining
- Circuit breaker patterns for Redis failures
- Comprehensive error handling and logging

### Security
- Input validation with Jakarta Bean Validation
- Secure random number generation
- No sensitive data in tracking numbers

### Operations
- Docker-ready with multi-stage builds
- Kubernetes health check endpoints
- Structured logging with trace correlation
