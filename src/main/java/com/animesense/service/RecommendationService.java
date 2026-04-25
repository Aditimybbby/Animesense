package com.animesense.service;

import com.animesense.model.Anime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {
    
    private final JikanApiService jikanApiService;
    private final AnimeService animeService;
    private final FavoriteService favoriteService;
    private final TasteProfileService tasteProfileService;
    
    @Value("${recommendation.min-rating}")
    private double minRating;
    
    public List<Anime> getRecommendations(Long userId, String mode, int count) {
        return switch (mode.toLowerCase()) {
            case "smart" -> getSmartRecommendations(userId, count);
            case "trending" -> getTrendingRecommendations(userId, count);
            case "basic" -> getBasicRecommendations(userId, count);
            default -> getSmartRecommendations(userId, count);
        };
    }
    
    public List<Anime> getSmartRecommendations(Long userId, int count) {
        List<String> topGenres = tasteProfileService.getTopGenres(userId, 5);
        
        if (topGenres.isEmpty()) {
            return getBasicRecommendations(userId, count);
        }
        
        List<Long> favoriteIds = favoriteService.getUserFavoriteAnimeIds(userId);
        List<Anime> allCandidates = new ArrayList<>();
        
        // Get anime for top 3 genres
        for (String genre : topGenres.subList(0, Math.min(3, topGenres.size()))) {
            List<Anime> genreAnime = jikanApiService.getAnimeByGenres(List.of(genre), 10);
            for (Anime anime : genreAnime) {
                animeService.addOrUpdateAnime(anime);
            }
            allCandidates.addAll(genreAnime);
        }
        
        // Remove duplicates and already favorited
        Map<Long, Anime> uniqueAnime = new HashMap<>();
        for (Anime anime : allCandidates) {
            if (!favoriteIds.contains(anime.getAnimeId()) && 
                (anime.getRating() == null || anime.getRating() >= minRating)) {
                uniqueAnime.put(anime.getAnimeId(), anime);
            }
        }
        
        // Score and sort
        List<ScoredAnime> scored = uniqueAnime.values().stream()
            .map(anime -> new ScoredAnime(anime, calculateScore(userId, anime)))
            .sorted(Comparator.comparingDouble(ScoredAnime::score).reversed())
            .limit(count)
            .toList();
        
        return scored.stream().map(ScoredAnime::anime).collect(Collectors.toList());
    }
    
    public List<Anime> getTrendingRecommendations(Long userId, int count) {
        List<Anime> topAnime = jikanApiService.getTopAnime(count * 2);
        List<Long> favoriteIds = favoriteService.getUserFavoriteAnimeIds(userId);
        
        return topAnime.stream()
            .peek(animeService::addOrUpdateAnime)
            .filter(anime -> !favoriteIds.contains(anime.getAnimeId()))
            .limit(count)
            .collect(Collectors.toList());
    }
    
    public List<Anime> getBasicRecommendations(Long userId, int count) {
        List<String> popularGenres = Arrays.asList("Action", "Adventure", "Comedy", "Drama", "Fantasy");
        Collections.shuffle(popularGenres);
        
        List<String> selectedGenres = popularGenres.subList(0, Math.min(3, popularGenres.size()));
        List<Anime> candidates = jikanApiService.getAnimeByGenres(selectedGenres, count * 3);
        List<Long> favoriteIds = favoriteService.getUserFavoriteAnimeIds(userId);
        
        return candidates.stream()
            .peek(animeService::addOrUpdateAnime)
            .filter(anime -> !favoriteIds.contains(anime.getAnimeId()))
            .filter(anime -> anime.getRating() == null || anime.getRating() >= minRating)
            .limit(count)
            .collect(Collectors.toList());
    }
    
    public List<Anime> getRecommendationsByGenres(Long userId, List<String> genres, int count) {
        List<Anime> candidates = jikanApiService.getAnimeByGenres(genres, count * 2);
        List<Long> favoriteIds = favoriteService.getUserFavoriteAnimeIds(userId);
        
        return candidates.stream()
            .peek(animeService::addOrUpdateAnime)
            .filter(anime -> !favoriteIds.contains(anime.getAnimeId()))
            .filter(anime -> anime.getRating() == null || anime.getRating() >= minRating)
            .limit(count)
            .collect(Collectors.toList());
    }
    
    private double calculateScore(Long userId, Anime anime) {
        double score = 0.0;
        
        // Base score from rating
        if (anime.getRating() != null) {
            score += anime.getRating() * 10;
        }
        
        // Genre match bonus
        for (String genre : anime.getGenreList()) {
            double weight = tasteProfileService.getGenreWeight(userId, genre.trim());
            score += weight * 20;
        }
        
        // Admin boost
        score *= anime.getAdminBoost();
        
        // Featured bonus
        if (anime.isFeatured()) {
            score *= 1.5;
        }
        
        return score;
    }
    
    private record ScoredAnime(Anime anime, double score) {}
}
