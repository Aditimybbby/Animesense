package com.animesense.repository;

import com.animesense.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT u FROM User u WHERE u.lastActive >= :cutoffDate")
    List<User> findActiveUsersSince(LocalDateTime cutoffDate);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastActive >= :cutoffDate")
    long countActiveUsersSince(LocalDateTime cutoffDate);
    
    List<User> findByIsAdminTrue();
}
