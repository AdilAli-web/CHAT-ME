# CHAT-ME 💬

CHAT-ME is a one-to-one chat backend built with Java 21, Spring Boot, Spring Data JPA, MySQL, Spring Security, and WebSocket APIs.

> **Status: In development.** The repository contains foundations for registration, user search, conversation previews, paginated message history, and WebSocket delivery. Some flows still need configuration, fixes, and end-to-end tests.

## Why this project?

A chat backend must do more than send text. It must identify users, validate requests, store messages so they survive disconnects, load only the data needed by the client, and deliver new messages to connected users. CHAT-ME uses separate layers so each responsibility has a clear home.

## Technology

- Java 21 and Spring Boot
- Spring Web / REST
- Spring Data JPA and Hibernate
- MySQL
- Spring Security and BCrypt password encoding
- Spring WebSocket
- Jakarta Validation
- Maven and Lombok

## Architecture

```text
Frontend / HTTP client
        |
        v
ChatController  -> receives HTTP requests and returns responses
        |
        v
ChatService     -> applies application rules and coordinates work
        |
        +------> AccountRepo ------> Account table
        |
        +------> ChatMessageRepo --> Chat messages table

Connected clients <---- LiveUpdates (WebSocket handler)
```

- **Controller:** handles HTTP requests and obtains the authenticated user's `Principal`.
- **Service:** implements registration, search, conversation list, history, and send-message logic.
- **Repository:** reads/writes database records using Spring Data JPA.
- **Entity:** maps Java objects to database tables.
- **DTO:** defines request/response data.
- **Exception handling:** turns validation and application errors into HTTP responses.
- **LiveUpdates:** keeps a concurrent collection of WebSocket sessions and publishes new messages to the sender and recipient.

## How the main functionality works

### Register — `POST /api/register`

The controller validates the `Registration` DTO with `@Valid`. The service trims input, checks for an existing username, encodes the password with BCrypt, saves an `Account`, and returns a `Person` DTO. This keeps passwords hashed rather than stored as plain text and keeps business logic out of the controller.

Example request:
```json
{
  "userName": "adil",
  "displayName": "Adil Ali",
  "password": "use-a-strong-password",
  "phoneNo": "9876543210"
}
```

### Current profile — `GET /api/me`

The controller takes the username from Spring Security's authenticated `Principal`. The service loads the account and maps it to a profile DTO. The backend should use the principal rather than trust a username supplied by the client.

### Search users — `GET /api/users?query=rah`

The service rejects blank or malformed prefixes and calls the repository for up to 20 usernames beginning with that prefix, excluding the current user. Prefix search, database indexing, a result limit, and frontend debouncing help avoid loading every account into memory.

### Conversation list — `GET /api/conversations`

The service gets candidate latest messages, determines the other participant, batch-loads the participant accounts, and builds `Conversation` DTOs. This populates the chat list with each peer and a latest-message preview; it does not load all messages from all chats.

### Message history — `GET /api/message/{peer}?before={messageId}`

The repository selects messages exchanged in both directions between the current user and the selected peer, with IDs lower than `before`, ordered newest-first. The service requests up to 50 rows and reverses the page so messages are returned oldest-first for display. The first request needs an initial cursor such as `Long.MAX_VALUE`; later requests should pass the oldest message ID from the previous page.

### Sending and live delivery

The service has a `send(OutGoing, currentUserName)` method that saves a message and calls `LiveUpdates.publish()`. The WebSocket handler serializes the message as JSON and sends it to active sessions authenticated as either participant. Database storage preserves history; WebSocket delivery gives connected users an immediate update.

**Not yet wired end-to-end:** the checked-in controller does not expose a send-message endpoint, and there is no WebSocket configuration class registering the handler at a URL. The current service publishes before its transaction commits; production-safe delivery should happen after a successful commit.

## API endpoints currently present

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/csrf` | Returns the CSRF token and header name |
| POST | `/api/register` | Validates and registers an account |
| GET | `/api/me` | Gets the authenticated user's profile |
| GET | `/api/users?query={prefix}` | Searches up to 20 users |
| GET | `/api/conversations` | Gets conversation previews |
| GET | `/api/message/{peer}?before={id}` | Gets a page of chat history |

The current `SecurityConfig` provides a BCrypt encoder but does not define an explicit `SecurityFilterChain` or a complete authentication flow.

## Data model

- **Account:** username (primary key), display name, phone number, password hash, and a version field. The phone number is currently required and unique, so sharing a phone number between accounts is not allowed.
- **ChatMessage:** generated ID, sender, recipient, body, sent time, and creation time. Database indexes are declared to support message queries.
- **Registration:** incoming fields with validation constraints.
- **OutGoing:** recipient and body for a message request.
- **Person:** profile data returned to clients.
- **Conversation:** the latest message plus the other participant.
- **ExceptionResponse:** timestamp, HTTP status, message, and request path for errors.

## Run locally

Prerequisites: JDK 21 and MySQL.

```bash
git clone https://github.com/AdilAli-web/CHAT-ME.git
cd CHAT-ME/CHAT-ME
```

Configure the database connection and JPA settings for your environment. The checked-in `application.properties` currently only sets the application name. Do not commit database credentials.

Windows:
```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

macOS/Linux:
```bash
./mvnw spring-boot:run
./mvnw test
```

## Known gaps and next steps

- Fix the two-argument `Person` constructor: it currently does not assign its fields, but search uses it, so search results can contain empty profile data.
- Fix conversation grouping so messages in both directions produce one latest message per peer. The repository currently groups sender/recipient directionally, and service deduplication may keep an older message.
- Validate blank/oversized message bodies and verify recipients exist before saving.
- Ensure the authenticated username is used for send and history operations.
- Publish WebSocket messages after database commit.
- Register the WebSocket handler and define the intended security/authentication configuration.
- Do not expose phone numbers or password hashes in public responses; use narrow DTOs/projections.
- Handle database constraint errors accurately: a conflict may be caused by a duplicate phone number, not just a username.
- Add integration tests for registration, search, history pagination, authorization, and live delivery.

## Author

**Adil Ali** · [GitHub](https://github.com/AdilAli-web)
