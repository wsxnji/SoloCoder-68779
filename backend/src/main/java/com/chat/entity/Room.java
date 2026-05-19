package com.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @Column(length = 6)
    private String roomNumber;
    
    @Column(length = 50)
    private String password;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
