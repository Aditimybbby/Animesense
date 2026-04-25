package com.animesense.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorites")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "anime_id", nullable = false)
    private Anime anime;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FavoriteStatus status = FavoriteStatus.PLAN_TO_WATCH;
    
    private Double userRating; // 1-10
    
    @Column(nullable = false)
    private LocalDateTime addedAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum FavoriteStatus {
        WATCHING("Watching"),
        COMPLETED("Completed"),
        PLAN_TO_WATCH("Plan to Watch"),
        DROPPED("Dropped");
        
        private final String displayName;
        
        FavoriteStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
