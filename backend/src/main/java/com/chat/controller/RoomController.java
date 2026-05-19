package com.chat.controller;

import com.chat.entity.Message;
import com.chat.entity.User;
import com.chat.service.RoomService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class RoomController {
    
    private final RoomService roomService;
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final String UPLOAD_DIR = "uploads/images";
    
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createRoom(@RequestBody(required = false) CreateRoomRequest request) {
        String password = request != null ? request.getPassword() : null;
        String roomNumber = roomService.createRoom(password);
        Map<String, Object> response = new HashMap<>();
        response.put("roomNumber", roomNumber);
        response.put("hasPassword", password != null && !password.trim().isEmpty());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check/{roomNumber}")
    public ResponseEntity<Map<String, Object>> checkRoom(@PathVariable String roomNumber) {
        boolean exists = false;
        boolean passwordRequired = false;
        if (roomNumber != null && roomNumber.trim().length() == 6) {
            exists = roomService.isRoomExists(roomNumber.trim());
            if (exists) {
                passwordRequired = roomService.isPasswordRequired(roomNumber.trim());
            }
        }
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("passwordRequired", passwordRequired);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/join")
    public ResponseEntity<?> joinRoom(@RequestBody JoinRequest request) {
        try {
            User user = roomService.joinRoom(request.getRoomNumber(), request.getNickname(), request.getSessionId(), request.getPassword());
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
    
    @GetMapping("/messages/{roomNumber}")
    public ResponseEntity<List<Map<String, Object>>> getRecentMessages(
            @PathVariable String roomNumber,
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(required = false) Long beforeId) {
        List<Message> messages;
        if (beforeId != null) {
            messages = roomService.getMessagesBeforeId(roomNumber, beforeId, count);
        } else {
            messages = roomService.getRecentMessages(roomNumber, count);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", msg.getId());
            item.put("type", msg.getType());
            item.put("content", msg.getContent());
            item.put("sender", msg.getSenderId());
            item.put("roomNumber", msg.getRoomId());
            item.put("timestamp", msg.getCreatedAt().toString());
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "文件不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (file.getSize() > MAX_IMAGE_SIZE) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "图片大小不能超过10MB");
            return ResponseEntity.badRequest().body(response);
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "只能上传图片文件");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".png";
            String newFilename = UUID.randomUUID().toString() + extension;
            
            Path path = Paths.get(UPLOAD_DIR, newFilename);
            Files.write(path, file.getBytes());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("url", "/api/room/images/" + newFilename);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "图片上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/images/{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) {
        try {
            Path path = Paths.get(UPLOAD_DIR, filename);
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }
            byte[] imageBytes = Files.readAllBytes(path);
            String contentType = Files.probeContentType(path);
            return ResponseEntity.ok()
                    .header("Content-Type", contentType != null ? contentType : "image/png")
                    .body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Data
    public static class CreateRoomRequest {
        private String password;
    }
    
    @Data
    public static class JoinRequest {
        private String roomNumber;
        private String nickname;
        private String sessionId;
        private String password;
    }
    
    @Data
    public static class LeaveRequest {
        private String roomNumber;
        private String sessionId;
    }
}
