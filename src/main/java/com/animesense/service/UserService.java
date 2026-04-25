package com.animesense.service;

import com.animesense.model.User;
import com.animesense.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    @Value("${admin.telegram.ids}")
    private String adminTelegramIds;
    
    @Transactional
    public User createOrUpdateUser(Long userId, String username, String firstName) {
        Optional<User> existingUser = userRepository.findById(userId);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setUsername(username);
            user.setFirstName(firstName);
            user.setLastActive(LocalDateTime.now());
            return userRepository.save(user);
        } else {
            User newUser = new User();
            newUser.setUserId(userId);
            newUser.setUsername(username);
            newUser.setFirstName(firstName);
            newUser.setAdmin(isAdminUser(userId));
            return userRepository.save(newUser);
        }
    }
    
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public List<User> getActiveUsers(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return userRepository.findActiveUsersSince(cutoff);
    }
    
    public long getTotalUserCount() {
        return userRepository.count();
    }
    
    public long getActiveUserCount(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return userRepository.countActiveUsersSince(cutoff);
    }
    
    public boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
            .map(User::isAdmin)
            .orElse(false);
    }
    
    private boolean isAdminUser(Long userId) {
        if (adminTelegramIds == null || adminTelegramIds.isEmpty()) {
            return false;
        }
        
        return Arrays.stream(adminTelegramIds.split(","))
            .map(String::trim)
            .filter(id -> !id.isEmpty())
            .anyMatch(id -> id.equals(userId.toString()));
    }
    
    @Transactional
    public void updateLastActive(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastActive(LocalDateTime.now());
            userRepository.save(user);
        });
    }
}
