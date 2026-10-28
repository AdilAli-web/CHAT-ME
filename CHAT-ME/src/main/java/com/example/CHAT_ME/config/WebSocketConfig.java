package com.example.CHAT_ME.config;

import com.example.CHAT_ME.webSocket.LiveUpdates;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
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



    public WebSocketConfig(LiveUpdates updates, String[] frontendOrigins) {
        this.updates = updates;
        this.frontendOrigins = frontendOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
registry.addHandler(updates,"/ws").setAllowedOrigins(frontendOrigins)
        .addInterceptors(new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
             if(!(request instanceof ServletServerHttpRequest servlet)) return false;
             var session=servlet.getServletRequest().getSession(false);
             if(servlet==null) return false;
             attributes.put("httpSession",servlet);
             return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, @Nullable Exception exception) {
                if (exception != null) {
                    System.err.println(
                            "WebSocket handshake failed: "
                                    + exception.getMessage());
                }
            }
        });


    }

}
