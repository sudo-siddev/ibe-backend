# Review Rating Backend

Backend service for Review & Rating System - a standalone component of a multi-tenant Internet Booking Engine (IBE).

## Overview

This is a Spring Boot backend service that provides REST APIs for managing reviews and ratings. The service implements feature toggles at both global and hotel-type levels, ensuring flexible control over review functionality.

## Technology Stack

- **Java**: 17+
- **Spring Boot**: 3.2.0
- **Spring Data JPA**: For database access
- **Spring Security**: Basic authentication
- **PostgreSQL**: Database (schema assumed to exist)
- **AWS Systems Manager Parameter Store**: For feature toggles
- **Lombok**: For reducing boilerplate code
- **HikariCP**: Connection pooling
- **JUnit 5 + Mockito**: Testing framework

## Project Structure

```
com.booking.reviews
 ├── config          # Configuration classes (AWS, Security)
 ├── controller      # REST controllers
 ├── service         # Business logic layer
 ├── repository      # Data access layer
 ├── entity          # JPA entities
 ├── dto             # Data transfer objects
 ├── exception       # Exception handling
 └── security        # Security configuration
```

## API Endpoints

### Health Check
- `GET /health` - Health check endpoint

### Reviews
- `POST /api/reviews` - Create a new review
- `GET /api/reviews/room/{roomId}` - Get reviews for a room (with pagination and sorting)
- `GET /api/reviews/stats/{roomId}` - Get review statistics for a room

### Configuration
- `GET /api/config/reviews?hotelId={hotelId}` - Get review configuration for a hotel

## Feature Toggle Logic

The write review feature is controlled by two levels:

1. **Global Kill Switch**: AWS Parameter Store parameter `/review-system/global/write-review-enabled`
2. **Hotel Type Level**: `reviewEnabled` flag in the `hotel_types` table

**Effective Rule**: Write Review is enabled ONLY when BOTH conditions are true:
- Global parameter == `true`
- Hotel Type `reviewEnabled` == `true`

If disabled, the API returns:
```json
{
  "code": "FEATURE_DISABLED",
  "message": "Reviews are disabled for this hotel type"
}
```

## Validation Rules

- Rating: Must be between 1-5
- Email: Must be valid email format
- Comment: Maximum 1000 characters
- Booking ID: Must exist and be unique per review
- Room ID: Must exist and be active

## Configuration

### Application Profiles

- `dev`: Development environment
- `qa`: QA environment  
- `prod`: Production environment
- `test`: Test environment (uses H2 in-memory database)

### Environment Variables

- `DB_HOST`: PostgreSQL host (default: localhost)
- `DB_PORT`: PostgreSQL port (default: 5432)
- `DB_NAME`: Database name
- `DB_USERNAME`: Database username (placeholder in code)
- `DB_PASSWORD`: Database password (placeholder in code)
- `AWS_REGION`: AWS region (default: us-east-1)
- `SECURITY_USERNAME`: Basic auth username (placeholder in code)
- `SECURITY_PASSWORD`: Basic auth password (placeholder in code)

## Building and Running

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL (schema must exist)

### Build
```bash
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

Or with profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Testing

Run all tests:
```bash
mvn test
```

Test coverage is generated using JaCoCo. View coverage report:
```bash
mvn jacoco:report
```

## Required PostgreSQL Schema

⚠️ **IMPORTANT**: The database schema is assumed to exist. The backend code maps to the following expected schema structure.

### Tables

#### `hotel_types`
Stores hotel type/category information with review enablement flag.

**Columns:**
- `id` (BIGSERIAL PRIMARY KEY)
- `name` (VARCHAR NOT NULL UNIQUE) - Hotel type name (e.g., "Luxury", "Budget")
- `review_enabled` (BOOLEAN NOT NULL DEFAULT false) - Whether reviews are enabled for this hotel type

**Indexes:**
- Primary key on `id`
- Unique index on `name`

#### `hotels`
Stores hotel information linked to hotel types.

**Columns:**
- `id` (BIGSERIAL PRIMARY KEY)
- `hotel_type_id` (BIGINT NOT NULL REFERENCES hotel_types(id))
- `name` (VARCHAR NOT NULL) - Hotel name
- `active` (BOOLEAN NOT NULL DEFAULT true) - Whether hotel is active

**Indexes:**
- Primary key on `id`
- Foreign key index on `hotel_type_id`
- Index on `active` (for filtering active hotels)

#### `rooms`
Stores room information linked to hotels.

**Columns:**
- `id` (BIGSERIAL PRIMARY KEY)
- `hotel_id` (BIGINT NOT NULL REFERENCES hotels(id))
- `room_number` (VARCHAR NOT NULL) - Room number/identifier
- `room_type` (VARCHAR) - Type of room (e.g., "Standard", "Suite")
- `active` (BOOLEAN NOT NULL DEFAULT true) - Whether room is active

**Indexes:**
- Primary key on `id`
- Foreign key index on `hotel_id`
- Index on `active` (for filtering active rooms)

#### `bookings`
Stores booking information linked to rooms.

**Columns:**
- `id` (BIGSERIAL PRIMARY KEY)
- `room_id` (BIGINT NOT NULL REFERENCES rooms(id))
- `guest_email` (VARCHAR NOT NULL) - Guest email address
- `status` (VARCHAR NOT NULL) - Booking status (e.g., "CONFIRMED", "CANCELLED")

**Indexes:**
- Primary key on `id`
- Foreign key index on `room_id`
- Index on `status` (for filtering by status)

#### `reviews`
Stores review and rating information.

**Columns:**
- `id` (BIGSERIAL PRIMARY KEY)
- `room_id` (BIGINT NOT NULL REFERENCES rooms(id))
- `booking_id` (BIGINT NOT NULL UNIQUE REFERENCES bookings(id)) - One review per booking
- `rating` (INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5)) - Rating from 1-5
- `comment` (VARCHAR(1000)) - Optional review comment (max 1000 chars)
- `reviewer_email` (VARCHAR NOT NULL) - Reviewer email
- `reviewer_name` (VARCHAR) - Optional reviewer name
- `created_at` (TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) - Review creation timestamp
- `updated_at` (TIMESTAMP) - Last update timestamp

**Indexes:**
- Primary key on `id`
- Foreign key index on `room_id` - **Critical for querying reviews by room**
- Unique index on `booking_id` - **Critical for enforcing one review per booking**
- Index on `created_at` - **Critical for sorting reviews by date**

### Index Rationale

1. **`idx_review_room_id`**: Essential for efficiently querying all reviews for a specific room (most common query pattern)
2. **`idx_review_booking_id`**: Unique constraint ensures one review per booking; also speeds up duplicate check queries
3. **`idx_review_created_at`**: Enables efficient sorting by creation date (default sort order for review listings)
4. **Foreign key indexes**: Automatically created by PostgreSQL for foreign key relationships, improving join performance

### Relationships

```
hotel_types (1) ──< (many) hotels (1) ──< (many) rooms (1) ──< (many) bookings
                                                                        │
                                                                        │ (1:1)
                                                                        ▼
                                                                    reviews
```

### Constraints

- One review per booking (enforced by unique constraint on `booking_id`)
- Rating must be between 1-5 (enforced by CHECK constraint)
- Comment maximum length 1000 characters (enforced by VARCHAR(1000))
- All foreign key relationships must be valid
- Only active rooms and hotels can receive reviews (enforced by application logic)

## Security

- Basic authentication is enabled for all endpoints except `/health`
- Credentials are configured via environment variables (placeholders in code)
- Stateless session management

## Logging

- SLF4J with configurable log levels per profile
- Structured logging with timestamps
- SQL logging available in dev profile

## Error Handling

Centralized exception handling via `GlobalExceptionHandler`:
- `FEATURE_DISABLED`: Feature toggle disabled (403)
- `RESOURCE_NOT_FOUND`: Resource not found (404)
- `DUPLICATE_REVIEW`: Review already exists for booking (409)
- `VALIDATION_ERROR`: Request validation failed (400)
- `INTERNAL_ERROR`: Unexpected server error (500)

## Next Steps

After backend implementation is approved:

1. **Database Schema**: Manual PostgreSQL schema design and migration scripts
2. **Schema Alignment**: Verify entity mappings match actual schema
3. **Frontend**: Implementation in separate `review-rating-frontend` repository
4. **AWS Deployment**: Infrastructure setup for Elastic Beanstalk deployment

## License

Proprietary - Internal use only

