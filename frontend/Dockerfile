# Build the React + Vite frontend
FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
# The production frontend uses Nginx same-origin proxy paths (/api and /ws).
ENV VITE_API_URL=
RUN npm run build

# Serve the static build and proxy API/WebSocket requests to Spring Boot.
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
