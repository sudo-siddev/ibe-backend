# Review Rating Backend

Spring Boot backend service for Review & Rating System - a standalone component of a multi-tenant Internet Booking Engine (IBE).

## Technology Stack

- Java 17+, Spring Boot 3.2.0
- Spring Data JPA, PostgreSQL
- Spring Security (Basic Auth)
- AWS Systems Manager Parameter Store (feature toggles)
- Swagger/OpenAPI (springdoc-openapi)
- Lombok, HikariCP

## API Endpoints

- `GET /health` - Health check (public)
- `POST /api/reviews` - Create review
- `GET /api/reviews/room/{roomId}` - Get reviews (pagination, sorting)
- `GET /api/reviews/stats/{roomId}` - Get review statistics
- `GET /api/config/reviews?hotelId={hotelId}` - Get review configuration

## API Documentation (Swagger)

Swagger UI is enabled in **dev** and **qa** profiles only. Disabled in production.

**Access:** `http://localhost:8080/swagger-ui.html`

**Features:**
- Interactive API testing
- Request/response examples
- Validation rules and error responses
- Basic Auth support

**Credentials (dev):** `placeholder_user` / `placeholder_password`

## Feature Toggles

Write review feature is controlled by two levels:

1. **Global Kill Switch**: AWS Parameter Store (`/review-system/global/write-review-enabled`)
2. **Hotel Type Level**: `review_enabled` flag in `hotel_types` table

**Rule:** Reviews enabled only when **BOTH** are true.

## Configuration

### Environment Variables

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- `AWS_REGION` (default: us-east-1)
- `SECURITY_USERNAME`, `SECURITY_PASSWORD`
- `SERVER_PORT` (default: 8080)

### Profiles

- `dev`: Local development (Swagger enabled, local override for feature toggle)
- `qa`: QA environment (Swagger enabled)
- `prod`: Production (Swagger disabled)
- `test`: Test environment (H2 in-memory database)

## Building and Running

### Prerequisites
- Java 17+, Maven 3.6+, PostgreSQL

### Build
```bash
mvn clean install
```

### Run
```bash
mvn spring-boot:run
# Or with profile:
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Testing
```bash
mvn test
mvn jacoco:report  # View coverage report
```

## Database Schema

The backend expects the following PostgreSQL schema to exist:

**Tables:** `hotel_types`, `hotels`, `rooms`, `bookings`, `reviews`

**Key Constraints:**
- One review per booking (unique constraint on `booking_id`)
- Rating: 1-5 (SMALLINT)
- Comment: max 1000 characters
- Indexes on `room_id`, `created_at`, `(room_id, rating)`

**Database Setup:**
- See `docs/grant_permissions.sql` for a template script to set up database user permissions
- **Note:** For AWS production, use RDS IAM authentication or manage users via infrastructure-as-code (Terraform/CloudFormation)
- The script is a template - replace placeholders with environment-specific values

## Security

- Basic authentication required for all `/api/**` endpoints
- `/health` and Swagger endpoints (when enabled) are public
- Stateless session management

## Error Codes

- `FEATURE_DISABLED` (403) - Feature toggle disabled
- `RESOURCE_NOT_FOUND` (404) - Resource not found
- `DUPLICATE_REVIEW` (409) - Review already exists
- `VALIDATION_ERROR` (400) - Request validation failed
- `INTERNAL_ERROR` (500) - Server error
