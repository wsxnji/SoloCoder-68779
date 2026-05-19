package com.chat.websocket;

import com.alibaba.fastjson2.JSON;
import com.chat.dto.ChatMessage;
import com.chat.entity.Message;
import com.chat.entity.User;
import com.chat.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {
    
    private final RoomService roomService;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String roomNumber = (String) session.getAttributes().get("roomNumber");
        String nickname = (String) session.getAttributes().get("nickname");
        String sessionId = (String) session.getAttributes().get("sessionId");
        
        roomSessions.computeIfAbsent(roomNumber, k -> new ConcurrentHashMap<>())
                .put(sessionId, session);
        
        sendHistoryMessages(session, roomNumber);
        
        ChatMessage joinMessage = new ChatMessage();
        joinMessage.setType("SYSTEM");
        joinMessage.setContent("用户 " + nickname + " 加入了房间");
        joinMessage.setSender("系统");
        joinMessage.setRoomNumber(roomNumber);
        joinMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        joinMessage.setSessionId(sessionId);
        
        sendMessageToRoom(roomNumber, JSON.toJSONString(joinMessage));
        
        broadcastUserList(roomNumber);
    }
    
    private void sendHistoryMessages(WebSocketSession session, String roomNumber) {
        try {
            List<Message> historyMessages = roomService.getRecentMessages(roomNumber, 5);
            for (Message msg : historyMessages) {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setType(msg.getType());
                chatMessage.setContent(msg.getContent());
                chatMessage.setSender(msg.getSenderId());
                chatMessage.setRoomNumber(msg.getRoomId());
                chatMessage.setTimestamp(msg.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                chatMessage.setSessionId(msg.getId().toString());
                
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(JSON.toJSONString(chatMessage)));
                }
            }
            
            ChatMessage historyEnd = new ChatMessage();
            historyEnd.setType("HISTORY_END");
            historyEnd.setContent("");
            historyEnd.setSender("系统");
            historyEnd.setRoomNumber(roomNumber);
            historyEnd.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(JSON.toJSONString(historyEnd)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        String roomNumber = (String) session.getAttributes().get("roomNumber");
        String nickname = (String) session.getAttributes().get("nickname");
        String sessionId = (String) session.getAttributes().get("sessionId");
        
        if (roomNumber == null || nickname == null || sessionId == null) {
            return;
        }
        
        try {
            String payload = message.getPayload().toString();
            ChatMessage chatMessage = JSON.parseObject(payload, ChatMessage.class);
            
            if (chatMessage == null || chatMessage.getContent() == null 
                || chatMessage.getContent().trim().isEmpty()) {
                return;
            }
            
            String msgType = chatMessage.getType() != null ? chatMessage.getType() : "CHAT";
            chatMessage.setSender(nickname);
            chatMessage.setRoomNumber(roomNumber);
            chatMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            chatMessage.setSessionId(sessionId);
            chatMessage.setType(msgType);
            
            roomService.saveMessage(roomNumber, nickname, chatMessage.getContent(), msgType);
            
            sendMessageToRoom(roomNumber, JSON.toJSONString(chatMessage));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        removeSession(session);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        removeSession(session);
    }
    
    private void removeSession(WebSocketSession session) {
        String roomNumber = (String) session.getAttributes().get("roomNumber");
        String nickname = (String) session.getAttributes().get("nickname");
        String sessionId = (String) session.getAttributes().get("sessionId");
        
        if (roomNumber == null || sessionId == null) {
            return;
        }
        
        ConcurrentHashMap<String, WebSocketSession> sessions = roomSessions.get(roomNumber);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomNumber);
            }
        }
        
        try {
            roomService.leaveRoom(roomNumber, sessionId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (nickname != null) {
            ChatMessage leaveMessage = new ChatMessage();
            leaveMessage.setType("SYSTEM");
            leaveMessage.setContent("用户 " + nickname + " 离开了房间");
            leaveMessage.setSender("系统");
            leaveMessage.setRoomNumber(roomNumber);
            leaveMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            leaveMessage.setSessionId(sessionId);
            
            sendMessageToRoom(roomNumber, JSON.toJSONString(leaveMessage));
        }
        
        broadcastUserList(roomNumber);
    }
    
    private void sendMessageToRoom(String roomNumber, String message) {
        ConcurrentHashMap<String, WebSocketSession> sessions = roomSessions.get(roomNumber);
        if (sessions != null) {
            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    private void broadcastUserList(String roomNumber) {
        ConcurrentHashMap<String, WebSocketSession> sessions = roomSessions.get(roomNumber);
        if (sessions != null) {
            List<User> users = roomService.getRoomUsers(roomNumber);
            
            ChatMessage userListMessage = new ChatMessage();
            userListMessage.setType("USER_LIST");
            userListMessage.setContent(JSON.toJSONString(users));
            userListMessage.setRoomNumber(roomNumber);
            userListMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            
            sendMessageToRoom(roomNumber, JSON.toJSONString(userListMessage));
        }
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
