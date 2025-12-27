# Tax Backend

A Spring Boot application for tax calculation and broker integration following enterprise Java best practices.

## Features

- **JWT Authentication** with AWS Cognito integration
- **Broker Integration** with OAuth2 flows (Coinbase)
- **RESTful API** with OpenAPI 3.0 documentation
- **Global Exception Handling** with standardized error responses
- **Input Validation** with Bean Validation (JSR-303)
- **Profile-based Configuration** for different environments
- **Comprehensive Security** headers and CSRF protection
- **Actuator Endpoints** for monitoring and health checks
- **Structured Logging** with SLF4J and Logback

## Architecture

This application follows a layered architecture with clear separation of concerns:

```
├── application/     # Controllers and DTOs
├── service/         # Business logic
├── infrastructure/  # External integrations
├── model/          # Domain entities
└── shared/         # Cross-cutting concerns
```

## API Documentation

Once the application is running, you can access the API documentation at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## Security

The application implements several security best practices:

- JWT-based stateless authentication
- HTTPS security headers
- Input validation on all endpoints
- Environment-based configuration
- Secure password policies
- CORS protection

## Configuration

### Environment Variables

The following environment variables are required:

```bash
# Database
DB_HOST=localhost
DB_NAME=taxooldb
DB_USERNAME=postgres
DB_PASSWORD=your_password

# AWS Cognito
COGNITO_POOL_ID=your_pool_id
COGNITO_CLIENT_ID=your_client_id
COGNITO_CLIENT_SECRET=your_client_secret
AWS_REGION=eu-central-1

# Coinbase Integration
COINBASE_CLIENT_ID=your_coinbase_client_id
COINBASE_CLIENT_SECRET=your_coinbase_client_secret
```

### Profiles

- `dev` - Development profile with debug logging and relaxed security
- `prod` - Production profile with optimized performance and security

## Running the Application

### Prerequisites

- Java 21
- PostgreSQL database
- Maven or Gradle

### Development

```bash
# Using Gradle
./gradlew bootRun --args='--spring.profiles.active=dev'

# Using Docker Compose
docker-compose up -d
```

### Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

## Monitoring

### Health Checks

- Health endpoint: `GET /actuator/health`
- Application info: `GET /actuator/info`
- Metrics: `GET /actuator/metrics`

### Logging

Application uses structured logging with the following levels:
- `ERROR` - System errors and exceptions
- `WARN` - Business logic warnings
- `INFO` - Important application events
- `DEBUG` - Detailed diagnostic information

## Contributing

### Code Style

- Follow Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Keep methods small and focused
- Use dependency injection consistently

### Testing

- Write unit tests for all business logic
- Use integration tests for API endpoints
- Mock external dependencies
- Aim for >80% code coverage

## Deployment

### Docker

```bash
docker build -t tax-backend .
docker run -p 8080:8080 tax-backend
```

### Production Considerations

- Use HTTPS in production
- Configure proper logging levels
- Set up monitoring and alerting
- Use environment-specific databases
- Enable security headers
- Configure proper CORS policies
