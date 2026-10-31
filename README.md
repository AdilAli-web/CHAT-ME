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
| `POST` | `/api/message` | Authenticated; validates and stores a message, then publishes after commit |

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

The repository query groups messages by the other participant using a normalized `CASE` expression, so messages in both directions are considered when selecting each peer's latest message. Add integration tests for this query with alternating sender/recipient directions.

### 5. Message history

`GET /api/message/{peer}?before={id}` returns up to 50 messages exchanged in either direction between the authenticated user and the selected peer. The repository orders by ID descending, and the service reverses the result so it is returned in ascending order for display. The first request can use a large initial cursor such as `Long.MAX_VALUE`; subsequent pages should pass the oldest message ID from the previous page.

### 6. WebSocket delivery

The WebSocket endpoint is registered at `/ws`. `LiveUpdates` rejects a WebSocket session without a principal, stores active sessions in a concurrent map, converts each message to JSON, and attempts to deliver it to active sessions whose principal matches the sender or recipient.

The handshake interceptor verifies the request principal, requires an existing HTTP session, and stores the actual `HttpSession` in the WebSocket attributes. Allowed frontend origins are injected from `app.frontend-origins` (defaulting to `http://localhost:5173`). Session expiry and reconnect behavior should still be tested in a real browser before production use.

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


---

# Recent additions: frontend, real-time chat, and Docker

The sections above document the original backend design and API. The project has since grown into a full-stack, containerized chat application. This section adds the newer implementation details while keeping the original README material intact.

## What's been added

### 1. React + Vite frontend

The `frontend/` directory contains a responsive, WhatsApp-inspired one-to-one messaging interface built with React 18, Vite, plain CSS, and Lucide icons.

Current frontend capabilities include:

- Login and account-registration screens connected to the backend.
- A responsive chat workspace with a conversation list and message view.
- Username-prefix people search, with a short input debounce.
- Opening a conversation and loading its message history.
- Sending messages through the REST API.
- Receiving live message events through the authenticated WebSocket connection.
- Conversation details, unread filters, mobile navigation, and a preview/demo mode.

The frontend uses `credentials: "include"` on HTTP requests so the browser sends the session cookie established by Spring Security. Its API base URL can be configured with `VITE_API_URL`; during local development it defaults to `http://localhost:8080`.

**Preview mode:** users can explore the interface with sample conversations when they do not have an authenticated backend session. Messages in preview mode are held in frontend React state; they are not saved to MySQL or delivered to another user.

**Not implemented yet:** attachment buttons, file/image sharing, voice calls, and video calls are UI placeholders. The project currently implements text messaging, not media transfer or calling.

### 2. Session authentication and browser security

The application uses Spring Security form login and an HTTP session rather than JWT authentication.

- Registration is handled by the application at `POST /api/register`.
- Login is processed by Spring Security at `POST /api/login` using form fields named `username` and `password`.
- Passwords are encoded with BCrypt before they are stored.
- `GET /api/csrf` exposes the CSRF token and header name for browser requests.
- The frontend sends the CSRF token on protected POST requests and includes browser credentials.
- The authenticated request principal is used by the backend to identify the current user; clients do not choose the sender identity.

The backend's CORS configuration allows the local Vite and frontend origins with credentials. The WebSocket handshake also checks the authenticated principal and an existing HTTP session. Keep the allowed origins narrow when changing deployment settings.

### 3. Real-time message delivery

The project combines REST APIs for durable message operations with WebSocket events for live updates.

1. The frontend submits a message to `POST /api/message`.
2. `ChatController` passes the request DTO and authenticated username to `ChatService`.
3. The service validates the recipient and message body, rejects self-messaging, and persists the message through `ChatMessageRepo`.
4. The service registers a transaction synchronization callback.
5. Only after the database transaction commits does the callback call `LiveUpdates.publish(...)`.
6. `LiveUpdates` serializes the saved message and sends it to active WebSocket sessions belonging to the sender or recipient.
7. The connected frontend processes the event and updates the relevant chat preview and open conversation.

This ordering is intentional: a live event is not published before the message has successfully committed to the database. REST remains the source for loading conversation history; WebSocket is used to notify already-connected clients about new messages.

The WebSocket endpoint is `/ws`. The handler maintains a concurrent registry of active connections and removes sessions when they close or become unusable. The handshake interceptor stores the associated HTTP session so the handler can check whether it is still valid.

### 4. Docker and Docker Compose

The repository now includes Dockerfiles for the backend and frontend plus a root `docker-compose.yml`. Compose starts three services:

| Service | Image/build | Responsibility | Host port |
|---|---|---|---|
| `db` | `mysql:8.4` | Persistent MySQL database | `3307` |
| `backend` | `./CHAT-ME` | Spring Boot REST API and WebSocket server | `8080` |
| `frontend` | `./frontend` | Nginx serving the built React app | `3000` |

The MySQL data directory is stored in the named `mysql_data` volume, so recreating containers does not normally remove the database contents. The backend waits for the database health check before starting.

Inside the Compose network, the backend connects to MySQL using the service hostname `db:3306`, not `localhost`. The frontend container serves the static Vite build through Nginx. Nginx proxies `/api/` requests to `backend:8080` and upgrades `/ws` requests to the backend WebSocket endpoint.

The backend Dockerfile uses a multi-stage build: Maven and Java 21 build the Spring Boot JAR, then a smaller Java 21 runtime image launches it. The frontend Dockerfile builds the React app with Node.js and copies the static output into an Nginx image.

#### Run the full stack with Docker Compose

From the repository root, create a local `.env` file containing the database password expected by Compose:

```env
DB_PASSWORD=change-this-to-a-local-password
```

Then run:

```bash
docker compose up --build
```

Open the frontend at [http://localhost:3000](http://localhost:3000). The backend is exposed at [http://localhost:8080](http://localhost:8080), and the MySQL host port is `3307`.

To stop the services:

```bash
docker compose down
```

To stop the services and also delete the persisted database volume (destructive):

```bash
docker compose down -v
```

Do not commit `.env` or real credentials. Use a strong local password and configure production secrets through the deployment environment rather than committing them to the repository.

## Full application flow

### A. First-time account registration

```text
React registration form
        |
        | POST /api/register (JSON + CSRF token)
        v
ChatController
        |
        v
ChatService.register()
        |
        +--> validate and normalize submitted values
        +--> check username uniqueness
        +--> BCrypt-encode password
        |
        v
AccountRepo.save()
        |
        v
MySQL accounts table
        |
        v
Person DTO returned to frontend
```

The password hash is stored in the account entity; the API response is a `Person` DTO and does not include the password.

### B. Login and application initialization

```text
React login form
        |
        +--> GET /api/csrf
        |
        +--> POST /api/login (form-encoded credentials + CSRF)
                         |
                         v
              Spring Security filter chain
                         |
                         +--> UserDetailsService loads Account
                         +--> BCrypt verifies submitted password
                         +--> HTTP session is established
                         |
                         v
              Frontend requests GET /api/me
                         |
                         v
              Load GET /api/conversations
                         |
                         v
              Open authenticated WebSocket /ws
```

After login, the browser retains the session cookie and sends it with subsequent API and WebSocket requests. The backend uses the principal attached to that session to determine who is making each request.

### C. Opening a chat

```text
User selects a conversation
        |
        v
GET /api/message/{peer}?before={cursor}
        |
        v
ChatController -> ChatService.history()
        |
        v
ChatMessageRepo.history(...)
        |
        v
MySQL: messages in either direction, ID below cursor
        |
        v
Up to 50 rows, newest-first from repository
        |
        v
Service reverses page for chronological display
        |
        v
React renders the conversation
```

History uses cursor-based pagination. The first page uses a large cursor; older pages can use the oldest message ID already loaded. The repository fetches at most 50 rows per page.

### D. Sending and receiving a live message

```text
Sender presses Send
        |
        v
POST /api/message
        |
        v
ChatController -> ChatService.send()
        |
        +--> validate recipient and body
        +--> reject self-message / unknown recipient
        +--> save message inside transaction
        |
        v
Transaction commits
        |
        v
afterCommit() -> LiveUpdates.publish()
        |
        +--> serialize saved message as JSON
        +--> find active sender/recipient WebSocket sessions
        +--> send event to matching connections
                          |
                          v
                 Recipient's React client
                          |
                          v
                Update chat list / open chat
```

The database commit happens before the WebSocket event is published. If the save transaction fails, the after-commit callback does not publish a message that was not committed.

## Updated architecture at a glance

```text
                  Browser
          React + Vite frontend
                  |
       HTTP REST  |  WebSocket /ws
                  |
           Nginx (Docker)
                  |
           Spring Boot API
       +----------+-----------+
       |          |           |
  Controller   Security   WebSocket handler
       |          |           |
       v          v           v
  ChatService  HTTP session  LiveUpdates
       |
  +----+----------------+
  |                     |
Repositories        after-commit
  |                 notification
  v                     |
 MySQL <----------------+
(accounts, chat_messages)
```

## Notes and current limitations

- The application is a one-to-one text chat. Group chats, message editing/deletion, delivery/read receipts, typing indicators, attachments, and calling are not implemented as backend features.
- The frontend contains preview/demo data. Preview messages are not persisted.
- WebSocket reconnect/backoff and end-to-end browser integration tests still need work.
- The current test suite is not a substitute for integration tests covering registration, CSRF, session authentication, CORS, history pagination, and live delivery.
- The local Compose database name is `chatme`; the local non-container `application.properties` database name is `chat`. Compose supplies an override JDBC URL for the containerized backend.
- When running frontend and backend separately, use the Vite URL and backend URL documented in `frontend/README.md`. When running through Compose, access the Nginx frontend on port `3000`.
