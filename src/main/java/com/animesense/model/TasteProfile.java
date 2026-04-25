package com.animesense.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "taste_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TasteProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 100)
    private String genre;
    
    @Column(nullable = false)
    private double weight = 1.0;
    
    @Column(nullable = false)
    private int interactionCount = 0;
    
    @Column(nullable = false)
    private double avgRating = 0.0;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
