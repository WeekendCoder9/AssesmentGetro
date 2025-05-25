TODO:
# Tracking Number Generator API

A scalable tracking number generator service built with Spring WebFlux and Redis.

## Prerequisites

- Java 17+
- Maven 3.6+
- Redis server

## Quick Start

1. **Start Redis locally:**
   ```bash
   redis-server
   ```

2. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

3. **Generate a tracking number:**
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

## API Endpoints

- `POST /api/v1/next-tracking-number` - Generate tracking number
- `GET /api/v1/health` - Health check
- `GET /actuator/health` - Detailed health status

## Testing

```bash
mvn test
```

## Configuration

Redis connection can be configured via environment variables:
- `REDIS_HOST` (default: localhost)
- `REDIS_PORT` (default: 6379)
