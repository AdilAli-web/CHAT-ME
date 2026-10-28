# CHAT-ME 💬

CHAT-ME is a one-to-one chat backend built with Java 21, Spring Boot, Spring Data JPA, Hibernate, MySQL, Spring Security, and WebSocket.

> **Project status: In development.** The repository has account registration, user search, conversation previews, paginated message history, session-based login configuration, CSRF token retrieval, and a WebSocket handler foundation. The HTTP send-message endpoint is wired in; WebSocket delivery is connected in the frontend. This is still a development project and needs integration testing before production use.

## Features in the codebase

- **Account registration** with Jakarta Bean Validation and BCrypt password hashing.
- **Session-based login configuration** using Spring Security form login at `POST /api/login`.
- **CSRF token endpoint** at `GET /api/csrf` for browser clients using session cookies.
- **Current profile** based on the authenticated request principal.
- **Username-prefix search** limited to 20 results and excluding the current user.
- **Conversation previews** assembled from recent messages and the other participant's account.
- **Bidirectional message history** between two users, fetched in pages of up to 50 messages.
- **WebSocket handler** at `/ws`, with a connection registry and JSON message publication to active sessions belonging to either participant.
- **Centralized API error handling** for validation failures, duplicate accounts, and request errors.

## Tech stack

- Java 21
- Spring Boot 4.1.2
- Spring Web / REST
- Spring Data JPA and Hibernate
- MySQL Connector/J
- Spring Security
- Spring WebSocket
- Jakarta Validation
- Lombok
- Maven
- JUnit / Spring Boot test dependencies

## Architecture

```text
HTTP client / frontend
        |
        v
ChatController  ----> ChatService
                          |
             +------------+-------------+
             |                          |
             v                          v
        AccountRepo                ChatMessageRepo
             |                          |
             v                          v
        Account table              Chat messages table

Browser session + Spring Security
        |
        +---- CSRF token endpoint: GET /api/csrf
        +---- Form login endpoint: POST /api/login

WebSocket client ----> /ws ----> LiveUpdates
                                  |
                                  +--> connected sender/recipient sessions
```

### Layer responsibilities

- **Controller:** Exposes HTTP endpoints and uses the authenticated `Principal` for user-specific operations.
- **Service:** Coordinates registration, search, conversation listing, history retrieval, and a message-send method.
- **Repositories:** Use Spring Data JPA to query accounts and messages.
- **Entities:** `Account` stores account data; `ChatMessage` stores message bodies and timestamps.
- **DTOs:** `Registration`, `Person`, `Conversation`, and `OutGoing` shape request and response data.
- **SecurityConfig:** Configures a security filter chain, form login, account lookup through `UserDetailsService`, and BCrypt password encoding.
- **WebSocketConfig:** Registers `LiveUpdates` at `/ws` and attaches a handshake interceptor.
- **LiveUpdates:** Tracks WebSocket sessions, serializes messages, and sends published messages to connected participants.
- **ApiErrors:** Maps selected application and validation exceptions to HTTP responses.

## HTTP API currently exposed

| Method | Endpoint | Authentication / purpose |
|---|---|---|
| `GET` | `/api/csrf` | Public; returns a CSRF token and header name for browser clients |
| `POST` | `/api/register` | Public; validates and creates an account |
| `POST` | `/api/login` | Form login endpoint handled by Spring Security; expects the default `username` and `password` form parameters |
| `GET` | `/api/me` | Authenticated; returns the current user's profile |
| `GET` | `/api/users?query={prefix}` | Authenticated; searches usernames by prefix |
| `GET` | `/api/conversations` | Authenticated; returns conversation previews |
| `GET` | `/api/message/{peer}?before={id}` | Authenticated; returns a history page with IDs below `before` |

`POST /api/message` accepts a validated `OutGoing` DTO and derives the sender from the authenticated principal. The service checks that the recipient exists, rejects self-messaging and blank bodies, saves the message, and publishes the WebSocket event after the transaction commits.

## Main request flows

### 1. Registration

1. The client submits `POST /api/register` with `userName`, `displayName`, `password`, and `phoneNo`.
2. Jakarta Validation checks required fields and constraints.
3. `ChatService` trims the text fields and checks whether the username already exists.
4. The password is encoded with BCrypt before the account is saved.
5. The API returns a `Person` response rather than the password hash.

Example body:

```json
{
  "userName": "adil",
  "displayName": "Adil Ali",
  "password": "use-a-strong-password",
  "phoneNo": "9876543210"
}
```

The phone number validation currently expects a 10-digit Indian mobile number beginning with 6–9, and the database model requires phone numbers to be unique.

### 2. Login and CSRF

Spring Security is configured for form login at `POST /api/login`. Successful login returns HTTP 204; failed login returns HTTP 401. The default form-login parameter names are `username` and `password`, and the current code does not add a JSON login converter.

The `GET /api/csrf` endpoint returns the current CSRF token and its header name. A browser frontend should request the token and include it in requests protected by CSRF, including the login POST. Session cookies should be handled consistently by the frontend.

### 3. Search

`GET /api/users?query=rah` returns up to 20 accounts whose usernames start with the provided prefix, excluding the signed-in user. Blank or malformed queries return an empty result. A frontend should debounce input to avoid issuing a request on every keystroke.

### 4. Conversation previews

`GET /api/conversations` fetches candidate latest messages, works out the other participant, loads participant accounts, and assembles `Conversation` DTOs. It is intended to return one preview per peer rather than every message in every chat.

**Known query limitation:** The repository groups by `sender, recipient` directionally. A message from A to B and a message from B to A can therefore be treated as separate groups. The service deduplicates peers after the query, but correctness depends on the query's ordering and can select the wrong preview in edge cases. This needs a query strategy that normalizes each participant pair.

### 5. Message history

`GET /api/message/{peer}?before={id}` returns up to 50 messages exchanged in either direction between the authenticated user and the selected peer. The repository orders by ID descending, and the service reverses the result so it is returned in ascending order for display. The first request can use a large initial cursor such as `Long.MAX_VALUE`; subsequent pages should pass the oldest message ID from the previous page.

### 6. WebSocket delivery

The WebSocket endpoint is registered at `/ws`. `LiveUpdates` rejects a WebSocket session without a principal, stores active sessions in a concurrent map, converts each message to JSON, and attempts to deliver it to active sessions whose principal matches the sender or recipient.

The handshake interceptor tries to attach HTTP-session data for later session validation. The implementation should be reviewed before production use: the current interceptor checks for a servlet request but tests `servlet == null` only after dereferencing it, does not check whether `getSession(false)` returned null, and stores the servlet request wrapper rather than the `HttpSession`. In addition, `frontendOrigins` is injected directly as a constructor argument, but no bean is defined for `String[]` in the checked-in configuration, so this dependency may prevent startup. Configure origins explicitly and correct the interceptor before relying on it.

## Database model

- **Account / `accounts`:** username primary key, display name, unique phone number, BCrypt password hash, and an optimistic-lock version field.
- **ChatMessage / `chat_messages`:** generated ID, sender, recipient, body, sent timestamp, and creation timestamp. Indexes are defined for sender/recipient/ID and recipient/ID query patterns.
- **Message history:** uses an ID cursor and a page size of 50 rather than returning an entire conversation at once.

## Configuration

The checked-in `application.properties` expects the MySQL database to be available locally at `127.0.0.1:3306`, creates the `chat` database if the MySQL account has the necessary permissions, and reads the database password from the `DB_PASSWORD` environment variable. It also configures Hibernate schema update, UTC JDBC time handling, strict Jackson deserialization options, an eight-hour session inactivity timeout, HTTP-only / SameSite Strict cookies, and generic default error responses.

Do not commit real database credentials. For local IntelliJ runs, set `DB_PASSWORD` in the Run Configuration environment variables. The current database username is configured as `root` in the properties file.

## Run locally

Prerequisites: JDK 21 and MySQL.

```bash
git clone https://github.com/AdilAli-web/CHAT-ME.git
cd CHAT-ME/CHAT-ME
```

Set `DB_PASSWORD` in your environment, ensure your MySQL server is running, and then run:

**Windows PowerShell**

```powershell
$env:DB_PASSWORD="your_mysql_password"
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

**macOS / Linux**

```bash
export DB_PASSWORD="your_mysql_password"
./mvnw spring-boot:run
./mvnw test
```

The repository currently contains a basic Spring Boot context-load test. A green test run does not yet demonstrate all login, CSRF, WebSocket, query-pagination, and message-delivery scenarios.

## Important follow-up work

- Add browser-level integration tests for CORS, CSRF, session login, WebSocket handshakes, and live delivery.
- Confirm the authenticated user identity is consistently used for all message operations.
- Add WebSocket reconnect/backoff behavior and integration tests for delivery to sender and recipient.
- Validate that recipients exist and that message bodies are nonblank and within the intended size limit.
- Review response DTOs and account privacy before production deployment.
- Fix latest-message query grouping so each peer appears exactly once with the true latest message across both directions.
- Review response DTOs to ensure phone numbers and persistence entities are not exposed unnecessarily.
- Improve duplicate-key error handling: a uniqueness conflict may be caused by the phone number, not only the username.
- Correct the `UserNotFound` exception message, which currently says the username already exists.
- Add integration tests for registration, login, CSRF, authorization, search, pagination, WebSocket handshakes, and live delivery.

## Author

**Adil Ali** · [GitHub](https://github.com/AdilAli-web)

## Frontend preview (React + Vite)

A WhatsApp-inspired responsive frontend is available in [frontend/](frontend/README.md). It includes a chat list, conversation view, message composer, search, registration, and session login UI.

Run it locally:

```bash
cd frontend
npm install
npm run dev
```

The UI uses preview data when the backend has no authenticated session; messages sent in preview mode are local to the browser session. See [frontend setup and integration notes](frontend/README.md) for backend URL, CORS, CSRF, and current limitations.
