package com.animesense.service;

import com.animesense.bot.AnimeSenseBot;
import com.animesense.model.BroadcastMessage;
import com.animesense.model.User;
import com.animesense.repository.BroadcastMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BroadcastService {
    
    private final BroadcastMessageRepository broadcastRepository;
    private final UserService userService;
    private final AnimeSenseBot bot;
    
    @Transactional
    public int sendBroadcastMessage(String message, Long adminUserId) {
        BroadcastMessage broadcast = new BroadcastMessage();
        broadcast.setAdminUserId(adminUserId);
        broadcast.setMessage(message);
        broadcast = broadcastRepository.save(broadcast);
        
        List<User> users = userService.getAllUsers();
        int sentCount = 0;
        int failedCount = 0;
        
        // FIXED: Safely convert any accidental leftover Markdown asterisks to HTML bold tags
        String safeMessage = message.replaceAll("\\*(.*?)\\*", "<b>$1</b>");
        safeMessage = safeMessage.replaceAll("_(.*?)_", "<i>$1</i>");
        
        for (User user : users) {
            try {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(user.getUserId());
                
                // FIXED: Switched from Markdown to HTML to prevent crashes on unescaped characters
                sendMessage.setText("📢 <b>Announcement</b>\n\n" + safeMessage);
                sendMessage.setParseMode("HTML");
                
                bot.execute(sendMessage);
                sentCount++;
            } catch (Exception e) {
                log.error("Failed to send broadcast to user {}: {}", user.getUserId(), e.getMessage());
                failedCount++;
            }
        }
        
        broadcast.setSentCount(sentCount);
        broadcast.setFailedCount(failedCount);
        broadcast.setCompletedAt(LocalDateTime.now());
        broadcastRepository.save(broadcast);
        
        return sentCount;
    }
    
    public List<BroadcastMessage> getBroadcastHistory() {
        return broadcastRepository.findAllByOrderByCreatedAtDesc();
    }
}