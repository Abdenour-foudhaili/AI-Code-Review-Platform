# Backend Architecture

The backend is built with Spring Boot and follows a layered architecture to ensure separation of concerns.

## Package Structure
- `controller/`: REST API endpoints and request routing.
- `service/`: Core business logic.
- `repository/`: Spring Data JPA interfaces for database access.
- `entity/`: JPA entities representing database tables.
- `dto/`: Data Transfer Objects for API requests and responses.
- `mapper/`: Classes to map between Entities and DTOs.
- `exception/`: Global exception handling and custom exception classes.
- `config/`: Configuration classes (e.g., CORS, Security, Beans).
- `ai/`: Abstracted interfaces and services for AI provider integration.
- `security/`: Security, JWT filters, authentication logic.
