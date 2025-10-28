# RAG Chat Storage Microservice

[![Java](https://img.shields.io/badge/Java-11-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A production-ready backend microservice for storing and managing chat histories from RAG (Retrieval-Augmented Generation) based chatbot systems with integrated AI assistant capabilities.

## Table of Contents

- [Features](#-features)
- [Technology Stack](#-technology-stack)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Running the Application](#-running-the-application)
- [API Documentation](#-api-documentation)
- [Database Schema](#-database-schema)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)
- [License](#-license)

## Features

### Core Functionality
- **Message Storage**: Store user and AI messages with context
- **Favorite Sessions**: Mark important conversations as favorites
- **Session Management**: Create, retrieve, update, and delete chat sessions
- **Session Renaming**: Update session titles for better organization
- **Message Retrieval**: Get conversation history with pagination support
- **AI Integration**: Optional AI assistant responses using HuggingFace

### Technical Features
- **Security**: API key authentication on all endpoints
- **Rate Limiting**: Prevent API abuse (configurable limits)
- **CORS Support**: Configurable cross-origin resource sharing
- **Request Validation**: Input validation using Bean Validation
- **Swagger Documentation**: Interactive API documentation
- **Health Checks**: Spring Actuator endpoints for monitoring
- **Pagination**: Efficient data retrieval for large datasets
- **Docker Support**: Complete containerization with Docker Compose
- **Database Management**: Adminer for easy database browsing
- **Centralized Logging**: Structured logging with SLF4J
- **Error Handling**: Global exception handler with meaningful errors

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Language** | Java | 11 |
| **Framework** | Spring Boot | 2.7.18 |
| **Database** | MySQL | 8.0 |
| **ORM** | Spring Data JPA / Hibernate | - |
| **API Documentation** | Springdoc OpenAPI (Swagger) | 1.7.0 |
| **Rate Limiting** | Bucket4j | 7.6.0 |
| **AI Integration** | HuggingFace Inference API | Free Tier |
| **Build Tool** | Maven | 3.8+ |
| **Containerization** | Docker & Docker Compose | Latest |
| **Testing** | JUnit 5, Mockito | - |

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 11** or higher ([Download](https://adoptopenjdk.net/))
- **Maven 3.6+** ([Download](https://maven.apache.org/download.cgi))
- **Docker** and **Docker Compose** ([Download](https://www.docker.com/get-started))
- **MySQL 8.0** (if running locally without Docker)
- **HuggingFace Account** (free) for AI features ([Sign up](https://huggingface.co/join))

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/nikhademinal/rag-chat-microservice.git
cd rag-chat-microservice
```

### 2. Configure Environment Variables

Copy the example environment file:

```bash
cp .env.example .env
```

Edit `.env` and update the following values:

```properties
# Database Configuration
DATABASE_USERNAME=your_db_user
DATABASE_PASSWORD=your_db_password
MYSQL_ROOT_PASSWORD=your_root_password

# Security - CHANGE THIS!
API_KEY=your-strong-api-key-here

# AI Assistant (Optional)
AI_ASSISTANT_ENABLED=true
HUGGINGFACE_API_KEY=your_huggingface_token

# Application Port
SERVER_PORT=8080
```

### 3. Get HuggingFace API Key (Optional - for AI features)

1. Sign up at [HuggingFace](https://huggingface.co/join)
2. Go to [Settings > Access Tokens](https://huggingface.co/settings/tokens)
3. Create a new token with "read" permission
4. Copy the token to your `.env` file

## Configuration

### Application Properties

The application uses environment variables for configuration. Key settings:

```properties
# Server
SERVER_PORT=8080

# Database
DATABASE_URL=jdbc:mysql://localhost:3306/ragchat_db
DATABASE_USERNAME=ragchat_user
DATABASE_PASSWORD=ragchat_password

# Security
API_KEY=your-api-key

# Rate Limiting
RATE_LIMIT_CAPACITY=100          # Max requests
RATE_LIMIT_REFILL_TOKENS=100     # Tokens to refill
RATE_LIMIT_REFILL_DURATION=60    # Refill every 60 seconds

# AI Assistant
AI_ASSISTANT_ENABLED=true
HUGGINGFACE_API_KEY=hf_xxxxx
AI_ASSISTANT_TIMEOUT=30000       # Timeout in milliseconds

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200
```

## Running the Application

### Option 1: Using Docker Compose (Recommended)

```bash
# Build and start all services
docker-compose up -d --build

# View logs
docker-compose logs -f app

# Check status
docker-compose ps

# Stop services
docker-compose down
```

**Access the application:**
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Adminer (DB): http://localhost:8081
- Health Check: http://localhost:8080/actuator/health

### Option 2: Running Locally

#### Start MySQL

```bash
# Using Docker for MySQL only
docker run -d \
  --name ragchat-mysql \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=ragchat_db \
  -e MYSQL_USER=ragchat_user \
  -e MYSQL_PASSWORD=ragchat_password \
  -p 3306:3306 \
  mysql:8.0
```

#### Run the Application

```bash
# Build the project
mvn clean package

# Run the JAR
java -jar target/rag-chat-microservice-1.0.0.jar

# Or use Maven
mvn spring-boot:run
```

## API Documentation

### Swagger UI

Interactive API documentation is available at:

**http://localhost:8080/swagger-ui.html**

### Authentication

All API endpoints require API key authentication (except health checks and Swagger).

**Add this header to all requests:**

```
X-API-Key: your-api-key-from-env-file
```

### API Endpoints

#### Session Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/chat/sessions` | Create a new chat session |
| `GET` | `/api/v1/chat/users/{userId}/sessions` | Get all sessions for a user |
| `GET` | `/api/v1/chat/users/{userId}/sessions/favorites` | Get favorite sessions |
| `GET` | `/api/v1/chat/sessions/{sessionId}` | Get session with all messages |
| `PUT` | `/api/v1/chat/sessions/{sessionId}/rename` | Rename a session |
| `PUT` | `/api/v1/chat/sessions/{sessionId}/favorite` | Toggle favorite status |
| `DELETE` | `/api/v1/chat/sessions/{sessionId}` | Delete a session |

#### Message Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/chat/sessions/{sessionId}/messages` | Send a message |
| `GET` | `/api/v1/chat/sessions/{sessionId}/messages` | Get all messages |
| `GET` | `/api/v1/chat/sessions/{sessionId}/messages/paginated` | Get paginated messages |

#### Health & Monitoring

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/actuator/health` | Application health status |
| `GET` | `/actuator/info` | Application information |

### Example Requests

#### Create a Session

```bash
curl -X POST http://localhost:8080/api/v1/chat/sessions \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "userId": "user123",
    "title": "My Chat Session"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Session created successfully",
  "data": {
    "id": 1,
    "userId": "user123",
    "title": "My Chat Session",
    "isFavorite": false,
    "createdAt": "2025-01-15T10:00:00",
    "updatedAt": "2025-01-15T10:00:00",
    "messageCount": 0
  }
}
```

#### Send a Message with AI Response

```bash
curl -X POST http://localhost:8080/api/v1/chat/sessions/1/messages \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "content": "Hello! How are you?",
    "context": "Greeting",
    "useAI": true
  }'
```

#### Get Session Messages (Paginated)

```bash
curl -X GET "http://localhost:8080/api/v1/chat/sessions/1/messages/paginated?page=0&size=20" \
  -H "X-API-Key: your-api-key"
```

## Database Schema

### Tables

#### chat_sessions

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT (PK) | Primary key |
| `user_id` | VARCHAR(255) | User identifier |
| `title` | VARCHAR(255) | Session title |
| `is_favorite` | BOOLEAN | Favorite flag |
| `created_at` | TIMESTAMP | Creation timestamp |
| `updated_at` | TIMESTAMP | Last update timestamp |

#### chat_messages

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT (PK) | Primary key |
| `session_id` | BIGINT (FK) | Reference to session |
| `sender` | ENUM | USER or AI_ASSISTANT |
| `content` | TEXT | Message content |
| `context` | TEXT | Optional context |
| `timestamp` | TIMESTAMP | Message timestamp |

### Entity Relationships

```
chat_sessions (1) ──── (N) chat_messages
    └── Cascade Delete: Deleting a session removes all its messages
```

### Accessing Database

**Using Adminer:**
1. Open: http://localhost:8081
2. Login with credentials from `.env`
3. Browse tables and data

**Using MySQL CLI:**
```bash
docker-compose exec mysql mysql -u ragchat_user -p
```

## Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=ChatServiceTest
```

### Run with Coverage

```bash
mvn test jacoco:report
```

View coverage report: `target/site/jacoco/index.html`

### Manual Testing

Access Swagger UI at http://localhost:8080/swagger-ui.html for interactive testing.

## Deployment

### Docker Deployment

```bash
# Build image
docker build -t ragchat-app:latest .

# Run with docker-compose
docker-compose -f docker-compose.prod.yml up -d

```

### Environment-Specific Configuration

Use Spring profiles for different environments:

```bash
# Development
java -jar app.jar --spring.profiles.active=dev

# Production
java -jar app.jar --spring.profiles.active=prod
```

## Project Structure

```
rag-chat-microservice/
├── src/
│   ├── main/
│   │   ├── java/com/ragchat/
│   │   │   ├── config/              # Configuration classes
│   │   │   │   ├── CorsConfig.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── controller/          # REST controllers
│   │   │   │   └── ChatController.java
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   └── DTOs.java
│   │   │   ├── entity/              # JPA entities
│   │   │   │   ├── ChatSession.java
│   │   │   │   └── ChatMessage.java
│   │   │   ├── exception/           # Custom exceptions
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── UnauthorizedException.java
│   │   │   │   └── RateLimitExceededException.java
│   │   │   ├── filter/              # Security filters
│   │   │   │   ├── ApiKeyAuthenticationFilter.java
│   │   │   │   └── RateLimitingFilter.java
│   │   │   ├── repository/          # JPA repositories
│   │   │   │   ├── ChatSessionRepository.java
│   │   │   │   └── ChatMessageRepository.java
│   │   │   ├── service/             # Business logic
│   │   │   │   ├── ChatService.java
│   │   │   │   └── AIAssistantService.java
│   │   │   └── RagChatMicroserviceApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/ragchat/
│           └── service/
│               └── ChatServiceTest.java
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── .env.example
├── .gitignore
├── .dockerignore
├── README.md