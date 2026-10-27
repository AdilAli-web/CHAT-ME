# CHAT-ME 💬

CHAT-ME is a chat application backend built with **Java and Spring Boot**. The project is currently under development. This README describes the work implemented so far and will be updated as more features are completed.

## Tech Stack

- **Java 21**
- **Spring Boot**
- **Spring Data JPA / Hibernate**
- **MySQL**
- **Spring Security**
- **WebSocket** for real-time communication
- **Maven**
- **Jakarta Validation**
- **Lombok**

## Current Implementation

### Accounts
- Account entity with username, display name, and password hash fields.
- Password encoder bean using BCrypt.
- Registration DTO created.
- Username-prefix search repository method for finding accounts.

### Chat Messages
- Chat message entity for storing sender, recipient, body, and timestamps.
- Database indexes intended to support conversation-history and recipient-based queries.
- Repository methods being developed for paginated chat history and conversation previews.

### Real-Time Communication
- A `LiveUpdates` WebSocket handler built on Spring's `TextWebSocketHandler`.
- Tracks active WebSocket sessions in a concurrent map.
- Serializes chat messages to JSON and attempts to deliver messages to connected sender/recipient sessions.
- Includes connection cleanup and session-validation logic.

## Project Structure

The Spring Boot application is located in the `CHAT-ME/` directory.

```text
CHAT-ME/
└── src/
    ├── main/
    │   ├── java/com/example/CHAT_ME/
    │   │   ├── Dtos/
    │   │   ├── Entity/
    │   │   ├── Exception/
    │   │   ├── Repo/
    │   │   ├── Services/
    │   │   ├── config/
    │   │   └── webSocket/
    │   └── resources/
    │       └── application.properties
    └── test/
```

## Getting Started

### Prerequisites

- JDK 21
- Maven (or use the included Maven Wrapper)
- MySQL

### Setup

1. Clone the repository:

   ```bash
   git clone https://github.com/AdilAli-web/CHAT-ME.git
   cd CHAT-ME/CHAT-ME
   ```

2. Configure your local database connection and JPA settings in `src/main/resources/application.properties`. Do not commit database passwords or other secrets.

3. Run the application with the Maven Wrapper:

   **Windows**
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   **macOS / Linux**
   ```bash
   ./mvnw spring-boot:run
   ```

> The project is still in development. Complete database configuration and WebSocket endpoint configuration may be needed before the application can run end to end.

## Roadmap

- [ ] Complete and test registration and login flows.
- [ ] Complete and test the authenticated WebSocket handshake and endpoint configuration.
- [ ] Connect the message-send service flow to persistence and live delivery.
- [ ] Finish and verify chat history pagination and conversation previews.
- [ ] Add integration tests for account and messaging flows.
- [ ] Build and connect a frontend.
- [ ] Improve production configuration, validation, and error handling.

## Project Status

**In progress** — foundational entities, repositories, dependencies, and a raw WebSocket handler are present. Features and end-to-end behavior are still being completed and verified.

## Author

**Adil Ali**

GitHub: [AdilAli-web](https://github.com/AdilAli-web)
