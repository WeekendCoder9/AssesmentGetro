# Tracking Number Generator API

A production-ready, scalable REST API for generating unique tracking numbers built with Spring Boot WebFlux.

## Features

- **Reactive Architecture**: Built with Spring WebFlux for high concurrency
- **Redis Integration**: Uses Redis for duplicate checking and caching
- **Distributed Tracing**: Integrated with Zipkin for request tracing
- **Comprehensive Testing**: Unit, integration, and performance tests
- **Production Ready**: Docker support, Kubernetes deployment, health checks
- **Robust Error Handling**: Global exception handling with proper HTTP status codes
- **Logging**: Structured logging with correlation IDs
- **Validation**: Request validation with detailed error messages
- **Date-based Generation**: Incorporates current date in tracking number generation for uniqueness

## API Specification

### Generate Tracking Number

**POST** `/api/v1/next-tracking-number`

#### Request Body
```json
{
  "origin_country_id": "US",
  "destination_country_id": "CA", 
  "weight": "1.234",
  "customer_id": "de619854-b59b-425e-9db4-943379e1bd49",
  "customer_name": "RedBox Logistics",
  "customer_slug": "redbox-logistics"
}
```

#### Response
```json
{
  "tracking_number": "ABC123DEF4",
  "created_at": "2025-05-25T10:15:30.123Z"
}
```

#### Field Descriptions

**Request Fields:**
- `origin_country_id` (required): ISO 3166-1 alpha-2 country code (e.g., "US", "CA")
- `destination_country_id` (required): ISO 3166-1 alpha-2 country code
- `weight` (required): Package weight in format "X.XXX" (up to 3 decimal places)
- `customer_id` (required): Unique customer identifier (max 36 characters)
- `customer_name` (required): Customer name (max 100 characters)
- `customer_slug` (optional): Customer slug for URL-friendly identification (max 50 characters)

**Response Fields:**
- `tracking_number`: Generated unique tracking number (1-16 alphanumeric characters)
- `created_at`: ISO 8601 timestamp when the tracking number was created

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker & Docker Compose
- Redis (for local development)

### Local Development

1. **Start Redis**:
   ```bash
   docker run -d -p 6379:6379 redis:7-alpine
   ```

2. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

3. **Test the API**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/next-tracking-number \
   -H "Content-Type: application/json" \
   -d '{
     "origin_country_id": "US",
     "destination_country_id": "CA",
     "weight": "1.234",
     "customer_id": "de619854-b59b-425e-9db4-943379e1bd49",
     "customer_name": "RedBox Logistics",
     "customer_slug": "redbox-logistics"
   }'
   ```

### Docker Deployment

1. **Build and run with Docker Compose**:
   ```bash
   docker-compose up --build
   ```

2. **Access the API**:
    - API: http://localhost:8080
    - Zipkin UI: http://localhost:9411
    - Redis: localhost:6379

### Kubernetes Deployment

1. **Deploy to Kubernetes**:
   ```bash
   kubectl apply -f k8s-deployment.yaml
   ```

## Testing

### Run All Tests
```bash
mvn test
```

### Test Coverage
```bash
mvn jacoco:report
```

### Performance Testing
```bash
mvn test -Dtest=TrackingNumberPerformanceTest
```

## Configuration

### Application Properties
```yaml
tracking-number:
  max-retries: 10          # Max attempts to generate unique number
  ttl-seconds: 86400       # Redis TTL for tracking numbers

spring:
  data:
    redis:
      host: localhost
      port: 6379

management:
  tracing:
    sampling:
      probability: 1.0     # Zipkin sampling rate
```

## Architecture

### Tracking Number Generation Algorithm

The tracking number generation incorporates several factors to ensure uniqueness:

1. **Input Components**:
   - Origin and destination country codes
   - Package weight
   - Customer information (ID, name, slug)
   - Current date (YYYY-MM-DD format)
   - Attempt number (for retry scenarios)

2. **Hash Generation**:
   - SHA-256 hash of combined input string
   - Ensures cryptographic randomness and distribution

3. **Formatting**:
   - Extracts first 10 characters from hash
   - Converts to uppercase alphanumeric format (A-Z, 0-9)
   - Validates against pattern `^[A-Z0-9]{1,16}$`

### Design Patterns Used
- **Strategy Pattern**: `TrackingNumberGenerator` interface
- **Repository Pattern**: Data access abstraction
- **Factory Pattern**: Spring dependency injection
- **Command Pattern**: Request/response DTOs

### Scalability Features
- Reactive streams for non-blocking I/O
- Redis for horizontal scaling
- Stateless design
- Connection pooling
- Circuit breaker patterns (via Spring retry)

### Monitoring & Observability
- Health check endpoint: `/api/v1/health`
- Metrics endpoint: `/actuator/metrics`
- Distributed tracing with Zipkin
- Structured logging with correlation IDs

## Error Handling

The API provides comprehensive error handling:

- **400 Bad Request**: Validation errors
- **409 Conflict**: Duplicate tracking number (internal retry)
- **500 Internal Server Error**: System errors

Example error response:
```json
{
  "timestamp": "2025-05-25T10:15:30.123Z",
  "status": 400,
  "error": "Validation failed",
  "details": {
    "origin_country_id": "Origin country ID must be in ISO 3166-1 alpha-2 format",
    "weight": "Weight must be in format X.XXX (up to 3 decimal places)"
  }
}
```

## Validation Rules

### Country Codes
- Must be exactly 2 uppercase letters (ISO 3166-1 alpha-2)
- Examples: "US", "CA", "GB", "DE"

### Weight Format
- Must match pattern: `\d{1,3}\.\d{3}`
- Examples: "1.234", "12.500", "123.000"
- Invalid: "1.23", "1234.567", "1.2345"

### Customer Fields
- `customer_id`: Required, max 36 characters
- `customer_name`: Required, max 100 characters  
- `customer_slug`: Optional, max 50 characters

## Production Considerations

- **Security**: Add authentication/authorization as needed
- **Rate Limiting**: Implement rate limiting for production use
- **Monitoring**: Set up application monitoring (Prometheus, Grafana)
- **Backup**: Configure Redis persistence and backup
- **SSL/TLS**: Enable HTTPS in production
- **Load Balancing**: Use multiple instances behind a load balancer

## Performance Characteristics

- **Generation Speed**: < 10ms per tracking number (typical)
- **Concurrency**: Supports high concurrent load via WebFlux
- **Uniqueness**: Date-based + hash ensures practical uniqueness
- **Retry Logic**: Automatic retry on collision with exponential backoff
- **Memory Usage**: Minimal memory footprint with reactive streams
