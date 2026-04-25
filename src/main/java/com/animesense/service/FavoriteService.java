package com.animesense.service;

import com.animesense.model.Anime;
import com.animesense.model.Favorite;
import com.animesense.model.Favorite.FavoriteStatus;
import com.animesense.model.User;
import com.animesense.repository.AnimeRepository;
import com.animesense.repository.FavoriteRepository;
import com.animesense.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteService {
    
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final AnimeRepository animeRepository;
    private final TasteProfileService tasteProfileService;
    
    @Transactional
    public Favorite addToFavorites(Long userId, Long animeId, FavoriteStatus status) {
        Optional<Favorite> existing = favoriteRepository.findByUserUserIdAndAnimeAnimeId(userId, animeId);
        
        if (existing.isPresent()) {
            Favorite favorite = existing.get();
            favorite.setStatus(status);
            return favoriteRepository.save(favorite);
        }
        
        User user = userRepository.findById(userId).orElseThrow();
        Anime anime = animeRepository.findById(animeId).orElseThrow();
        
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setAnime(anime);
        favorite.setStatus(status);
        
        // Update taste profile
        tasteProfileService.updateTasteProfile(userId, anime.getGenreList(), null);
        
        return favoriteRepository.save(favorite);
    }
    
    public List<Favorite> getUserFavorites(Long userId) {
        return favoriteRepository.findByUserUserId(userId);
    }
    
    public List<Favorite> getUserFavoritesByStatus(Long userId, FavoriteStatus status) {
        return favoriteRepository.findByUserUserIdAndStatus(userId, status);
    }
    
    public List<Long> getUserFavoriteAnimeIds(Long userId) {
        return favoriteRepository.findAnimeIdsByUserId(userId);
    }
    
    public boolean isInFavorites(Long userId, Long animeId) {
        return favoriteRepository.existsByUserUserIdAndAnimeAnimeId(userId, animeId);
    }
    
    @Transactional
    public void updateStatus(Long userId, Long animeId, FavoriteStatus status) {
        favoriteRepository.findByUserUserIdAndAnimeAnimeId(userId, animeId).ifPresent(favorite -> {
            favorite.setStatus(status);
            favoriteRepository.save(favorite);
        });
    }
    
    @Transactional
    public void updateRating(Long userId, Long animeId, Double rating) {
        favoriteRepository.findByUserUserIdAndAnimeAnimeId(userId, animeId).ifPresent(favorite -> {
            favorite.setUserRating(rating);
            favoriteRepository.save(favorite);
            
            // Update taste profile with rating
            tasteProfileService.updateTasteProfile(userId, favorite.getAnime().getGenreList(), rating);
        });
    }
    
    @Transactional
    public void removeFromFavorites(Long userId, Long animeId) {
        favoriteRepository.findByUserUserIdAndAnimeAnimeId(userId, animeId)
            .ifPresent(favoriteRepository::delete);
    }
    
    public long getUserFavoriteCount(Long userId) {
        return favoriteRepository.countByUserId(userId);
    }
    
    public long getUserFavoriteCountByStatus(Long userId, FavoriteStatus status) {
        return favoriteRepository.countByUserIdAndStatus(userId, status);
    }
    
    public Double getUserAverageRating(Long userId) {
        return favoriteRepository.findAverageRatingByUserId(userId);
    }
}
