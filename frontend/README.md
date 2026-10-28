# CHAT-ME Frontend 💬

A responsive React interface for CHAT-ME, styled as a calm, WhatsApp-inspired messaging workspace.

## Requirements

- Node.js 20+ recommended
- Spring Boot backend from the repository (Java 21 and MySQL)
- Backend available at `http://localhost:8080` by default

## Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open the local URL printed by Vite (normally `http://localhost:5173`).

To point the UI to another backend, copy `.env.example` to `.env` and set:

```env
VITE_API_URL=http://localhost:8080
```

Restart Vite after changing `.env`.

## Current UI

- Session login and account registration forms
- CSRF token retrieval before login, registration, and message sends
- Responsive chat list and one-to-one conversation view
- Search for existing chats and username-prefix people search
- Message history loading via `GET /api/message/{peer}?before=...`
- Sends via `POST /api/message`
- Receives live message events through the authenticated `/ws` WebSocket while signed in
- Profile details panel, unread filters, and mobile layout

If the backend is unavailable or there is no authenticated session, the app displays the login/register screen. Choose **Explore the interface with sample chats** to open **preview mode**. Preview-mode messages are stored only in React state and are not sent to the backend.

## Backend endpoints used

- `GET /api/csrf`
- `POST /api/register`
- `POST /api/login` (form-encoded `username` and `password`)
- `GET /api/me`
- `GET /api/conversations`
- `GET /api/users?query={prefix}`
- `GET /api/message/{peer}?before={id}`
- `POST /api/message`

## Notes

- Requests include browser credentials (`credentials: "include"`) to carry the session cookie.
- The backend must allow the frontend origin `http://localhost:5173` through its CORS configuration if frontend and backend run on different origins. The backend enables CORS in the Spring Security filter chain, allows only `http://localhost:5173`, and allows credentialed requests. Keep CSRF enabled and configure cookies consistently for your local environment.
- The backend's current Spring Security configuration does not show an explicit CORS setup. If API requests fail in the browser with a CORS error, add a carefully scoped CORS configuration for `http://localhost:5173` with credentials enabled. Do not use wildcard origins with credentials.
- The frontend opens `/ws` while signed in and updates the chat list/messages from received events. Browser-level testing is still needed; automatic reconnect/backoff is not implemented yet.
- File/image sharing, voice calls, and video calls are visual placeholders only; they are not implemented by the backend yet.
