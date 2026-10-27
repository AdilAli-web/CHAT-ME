package com.example.CHAT_ME.webSocket;

import com.example.CHAT_ME.Entity.ChatMessage;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class LiveUpdates extends TextWebSocketHandler {

    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    //convert object to string
    private final ObjectMapper mapper;

    public LiveUpdates(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if(session.getPrincipal()==null){
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        sessions.put(session.getId(),new ConcurrentWebSocketSessionDecorator(session,5000,65536));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
    }


    public void publish(ChatMessage message){

        var payload=new TextMessage(mapper.writeValueAsString(message));
        sessions.forEach((id,session)->{
            if(!validSession(session)){
                sessions.remove(id);
                try{
                    session.close(
                            CloseStatus.POLICY_VIOLATION
                    );
                }
                catch (IOException e){}
                return;
            }
            var name=session.getPrincipal().getName();
            if(name.equals(message.sender) || name.equals(message.recipient)){
                try{
                    session.sendMessage(payload);
                }
                catch (Exception e){
                    sessions.remove(id);
                    try{
                        session.close();
                    }
                    catch (IOException e2){}
                }
            }
        });
    }





public boolean validSession(WebSocketSession socket){
    try{
        var session = (HttpSession) socket.getAttributes().get("httpSession");
        return session!=null && (session.getMaxInactiveInterval()<=0 ||
                System.currentTimeMillis()-session.getLastAccessedTime()<session.getMaxInactiveInterval()*1000L);
    }
    catch (IllegalStateException e){
        return false;
    }
}



public void closeUser(String userName){
    sessions.forEach((id,session)->{
        if(session.getPrincipal().getName().equals(userName)){
            sessions.remove(id);
            try{
                session.close(
                        CloseStatus.NORMAL
                );
            }
            catch (IOException e){}
        }
    });
}
}
