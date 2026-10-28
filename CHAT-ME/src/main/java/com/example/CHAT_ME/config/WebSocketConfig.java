
package com.example.CHAT_ME.config;

import com.example.CHAT_ME.webSocket.LiveUpdates;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LiveUpdates updates;
    private final String[] frontendOrigins;

    public WebSocketConfig(
            LiveUpdates updates,
            @Value("${app.frontend-origins:http://localhost:5173}")
            String[] frontendOrigins) {
        this.updates = updates;
        this.frontendOrigins = frontendOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(updates, "/ws")
                .setAllowedOrigins(frontendOrigins)
                .addInterceptors(new HandshakeInterceptor() {

                    @Override
                    public boolean beforeHandshake(
                            ServerHttpRequest request,
                            ServerHttpResponse response,
                            WebSocketHandler handler,
                            Map<String, Object> attributes) {

                        if (!(request instanceof ServletServerHttpRequest servlet)) {
                            return false;
                        }

                        // Require an authenticated HTTP request.
                        if (request.getPrincipal() == null) {
                            response.setStatusCode(
                                    HttpStatus.UNAUTHORIZED
                            );
                            return false;
                        }

                        HttpSession session =
                                servlet.getServletRequest().getSession(false);

                        if (session == null) {
                            response.setStatusCode(
                                    HttpStatus.UNAUTHORIZED
                            );
                            return false;
                        }

                        // Store the actual HTTP session.
                        attributes.put("httpSession", session);
                        return true;
                    }

                    @Override
                    public void afterHandshake(
                            ServerHttpRequest request,
                            ServerHttpResponse response,
                            WebSocketHandler handler,
                            Exception exception) {

                        if (exception != null) {
                            System.err.println(
                                    "WebSocket handshake failed: "
                                            + exception.getMessage()
                            );
                        }
                    }
                });
    }
}
