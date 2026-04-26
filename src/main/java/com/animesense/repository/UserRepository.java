package com.animesense.repository;

import com.animesense.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // FIXED: Added @Param("cutoffDate") to explicitly map the variable to the query
    @Query("SELECT u FROM User u WHERE u.lastActive >= :cutoffDate")
    List<User> findActiveUsersSince(@Param("cutoffDate") LocalDateTime cutoffDate);

    // FIXED: Added @Param("cutoffDate") here as well
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastActive >= :cutoffDate")
    long countActiveUsersSince(@Param("cutoffDate") LocalDateTime cutoffDate);

    // FIXED: renamed from findByIsAdminTrue() to findByAdminTrue() to match field name 'admin'
    List<User> findByAdminTrue();
}