package com.chat.websocket;

import com.chat.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {
    
    private final RoomService roomService;
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        if (query == null || query.isEmpty()) {
            return false;
        }
        
        String roomNumber = null;
        String nickname = null;
        String sessionId = null;
        
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = decodeValue(keyValue[1]);
                if ("roomNumber".equals(key)) {
                    roomNumber = value;
                } else if ("nickname".equals(key)) {
                    nickname = value;
                } else if ("sessionId".equals(key)) {
                    sessionId = value;
                }
            }
        }
        
        if (roomNumber == null || roomNumber.trim().isEmpty() ||
            nickname == null || nickname.trim().isEmpty() ||
            sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        
        if (!roomService.isRoomExists(roomNumber)) {
            return false;
        }
        
        attributes.put("roomNumber", roomNumber.trim());
        attributes.put("nickname", nickname.trim());
        attributes.put("sessionId", sessionId.trim());
        return true;
    }
    
    private String decodeValue(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
