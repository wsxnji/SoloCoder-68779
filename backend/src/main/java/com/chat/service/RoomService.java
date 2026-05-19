package com.chat.service;

import com.chat.entity.Room;
import com.chat.entity.User;
import com.chat.repository.RoomRepository;
import com.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class RoomService {
    
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();
    
    public String createRoom() {
        String roomNumber;
        do {
            roomNumber = String.format("%06d", random.nextInt(1000000));
        } while (roomRepository.existsByRoomNumber(roomNumber));
        
        Room room = new Room();
        room.setRoomNumber(roomNumber);
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
    
    public User joinRoom(String roomNumber, String nickname, String sessionId) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new RuntimeException("昵称不能为空");
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new RuntimeException("会话ID无效");
        }
        if (!isRoomExists(roomNumber)) {
            throw new RuntimeException("房间不存在");
        }
        
        User user = new User();
        user.setNickname(nickname.trim());
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
}
