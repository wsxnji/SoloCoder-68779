package com.chat.controller;

import com.chat.entity.User;
import com.chat.service.RoomService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class RoomController {
    
    private final RoomService roomService;
    
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createRoom() {
        String roomNumber = roomService.createRoom();
        Map<String, String> response = new HashMap<>();
        response.put("roomNumber", roomNumber);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check/{roomNumber}")
    public ResponseEntity<Map<String, Boolean>> checkRoom(@PathVariable String roomNumber) {
        boolean exists = false;
        if (roomNumber != null && roomNumber.trim().length() == 6) {
            exists = roomService.isRoomExists(roomNumber.trim());
        }
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/join")
    public ResponseEntity<?> joinRoom(@RequestBody JoinRequest request) {
        try {
            User user = roomService.joinRoom(request.getRoomNumber(), request.getNickname(), request.getSessionId());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", user.getId());
            response.put("nickname", user.getNickname());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/leave")
    public ResponseEntity<Map<String, Boolean>> leaveRoom(@RequestBody LeaveRequest request) {
        roomService.leaveRoom(request.getRoomNumber(), request.getSessionId());
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/users/{roomNumber}")
    public ResponseEntity<List<User>> getRoomUsers(@PathVariable String roomNumber) {
        List<User> users = roomService.getRoomUsers(roomNumber);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/count/{roomNumber}")
    public ResponseEntity<Map<String, Integer>> getOnlineCount(@PathVariable String roomNumber) {
        int count = roomService.getOnlineCount(roomNumber);
        Map<String, Integer> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
    
    @Data
    public static class JoinRequest {
        private String roomNumber;
        private String nickname;
        private String sessionId;
    }
    
    @Data
    public static class LeaveRequest {
        private String roomNumber;
        private String sessionId;
    }
}
