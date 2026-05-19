package com.chat.repository;

import com.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByRoomNumber(String roomNumber);
    Optional<User> findBySessionIdAndRoomNumber(String sessionId, String roomNumber);
    void deleteBySessionIdAndRoomNumber(String sessionId, String roomNumber);
    void deleteByRoomNumber(String roomNumber);
}
