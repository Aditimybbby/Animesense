package com.animesense.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "anime")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Anime {

    @Id
    private Long animeId; // MyAnimeList ID

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 500)
    private String titleEnglish;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(length = 500)
    private String genres; // Comma-separated

    private Double rating;
    private Integer episodes;

    @Column(length = 100)
    private String status;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String trailerUrl;

    // FIXED: 'year' is a reserved keyword in H2 SQL — mapped to 'release_year' column
    @Column(name = "release_year")
    private Integer year;

    @Column(nullable = false)
    private boolean featured = false;

    @Column(nullable = false)
    private double adminBoost = 1.0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "anime", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public List<String> getGenreList() {
        if (genres == null || genres.isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(genres.split(","));
    }
}
