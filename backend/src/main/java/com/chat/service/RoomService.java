package com.chat.service;

import com.chat.entity.Message;
import com.chat.entity.Room;
import com.chat.entity.User;
import com.chat.repository.MessageRepository;
import com.chat.repository.RoomRepository;
import com.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {
    
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final Random random = new Random();
    
    public String createRoom(String password) {
        String roomNumber;
        do {
            roomNumber = String.format("%06d", random.nextInt(1000000));
        } while (roomRepository.existsByRoomNumber(roomNumber));
        
        Room room = new Room();
        room.setRoomNumber(roomNumber);
        if (password != null && !password.trim().isEmpty()) {
            room.setPassword(password.trim());
        }
        roomRepository.save(room);
        return roomNumber;
    }
    
    public boolean isRoomExists(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            return false;
        }
        String trimmed = roomNumber.trim();
        if (trimmed.length() != 6 || !trimmed.matches("\\d{6}")) {
            return false;
        }
        return roomRepository.existsByRoomNumber(trimmed);
    }
    
    public boolean isPasswordRequired(String roomNumber) {
        if (!isRoomExists(roomNumber)) {
            return false;
        }
        Optional<Room> roomOpt = roomRepository.findById(roomNumber.trim());
        return roomOpt.isPresent() && roomOpt.get().getPassword() != null && !roomOpt.get().getPassword().isEmpty();
    }
    
    public boolean verifyPassword(String roomNumber, String password) {
        if (!isRoomExists(roomNumber)) {
            return false;
        }
        Optional<Room> roomOpt = roomRepository.findById(roomNumber.trim());
        if (roomOpt.isEmpty()) {
            return false;
        }
        String roomPassword = roomOpt.get().getPassword();
        if (roomPassword == null || roomPassword.isEmpty()) {
            return true;
        }
        return roomPassword.equals(password != null ? password.trim() : null);
    }
    
    public User joinRoom(String roomNumber, String nickname, String sessionId, String password) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new RuntimeException("昵称不能为空");
        }
        String trimmedNickname = nickname.trim();
        if (trimmedNickname.length() < 2 || trimmedNickname.length() > 50) {
            throw new RuntimeException("昵称长度必须在2-50个字符之间");
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new RuntimeException("会话ID无效");
        }
        if (!isRoomExists(roomNumber)) {
            throw new RuntimeException("房间不存在");
        }
        if (!verifyPassword(roomNumber, password)) {
            throw new RuntimeException("房间密码错误");
        }
        
        User user = new User();
        user.setNickname(trimmedNickname);
        user.setRoomNumber(roomNumber.trim());
        user.setSessionId(sessionId);
        return userRepository.save(user);
    }
    
    @Transactional
    public void leaveRoom(String roomNumber, String sessionId) {
        if (roomNumber != null && sessionId != null) {
            userRepository.deleteBySessionIdAndRoomNumber(sessionId, roomNumber);
        }
    }
    
    public List<User> getRoomUsers(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            return List.of();
        }
        return userRepository.findByRoomNumber(roomNumber.trim());
    }
    
    public int getOnlineCount(String roomNumber) {
        return getRoomUsers(roomNumber).size();
    }
    
    public Message saveMessage(String roomId, String senderId, String content, String type) {
        Message message = new Message();
        message.setRoomId(roomId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setType(type != null ? type : "CHAT");
        return messageRepository.save(message);
    }
    
    public List<Message> getRecentMessages(String roomId, int count) {
        if (roomId == null || roomId.trim().isEmpty()) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, count);
        List<Message> messages = new ArrayList<>(messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId.trim(), pageable).getContent());
        Collections.reverse(messages);
        return messages;
    }
    
    public List<Message> getMessagesBeforeId(String roomId, Long beforeId, int count) {
        if (roomId == null || roomId.trim().isEmpty()) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, count + 5);
        List<Message> messages = new ArrayList<>(messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId.trim(), pageable).getContent());
        if (beforeId != null) {
            messages = messages.stream().filter(m -> m.getId() < beforeId).limit(count).collect(Collectors.toList());
        }
        Collections.reverse(messages);
        return messages;
    }
}
