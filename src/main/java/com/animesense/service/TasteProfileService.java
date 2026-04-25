package com.animesense.service;

import com.animesense.model.TasteProfile;
import com.animesense.model.User;
import com.animesense.repository.TasteProfileRepository;
import com.animesense.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TasteProfileService {
    
    private final TasteProfileRepository tasteProfileRepository;
    private final UserRepository userRepository;
    
    @Value("${recommendation.genre-weight-increment}")
    private double genreWeightIncrement;
    
    @Value("${recommendation.genre-weight-decrement}")
    private double genreWeightDecrement;
    
    @Transactional
    public void updateTasteProfile(Long userId, List<String> genres, Double rating) {
        User user = userRepository.findById(userId).orElseThrow();
        
        for (String genre : genres) {
            genre = genre.trim();
            if (genre.isEmpty()) continue;
            
            TasteProfile profile = tasteProfileRepository.findByUserUserIdAndGenre(userId, genre)
                .orElse(new TasteProfile());
            
            if (profile.getId() == null) {
                profile.setUser(user);
                profile.setGenre(genre);
                profile.setWeight(1.0);
                profile.setInteractionCount(0);
                profile.setAvgRating(0.0);
            }
            
            profile.setInteractionCount(profile.getInteractionCount() + 1);
            
            if (rating != null) {
                // Update average rating
                double totalRating = profile.getAvgRating() * (profile.getInteractionCount() - 1) + rating;
                profile.setAvgRating(totalRating / profile.getInteractionCount());
                
                // Adjust weight based on rating
                if (rating >= 7.0) {
                    profile.setWeight(profile.getWeight() + genreWeightIncrement);
                } else if (rating <= 4.0) {
                    profile.setWeight(Math.max(0.1, profile.getWeight() - genreWeightDecrement));
                }
            } else {
                // Just interaction without rating
                profile.setWeight(profile.getWeight() + (genreWeightIncrement * 0.5));
            }
            
            tasteProfileRepository.save(profile);
        }
    }
    
    public List<TasteProfile> getUserTasteProfile(Long userId) {
        return tasteProfileRepository.findTopGenresByUserId(userId);
    }
    
    public List<String> getTopGenres(Long userId, int limit) {
        List<String> genres = tasteProfileRepository.findTopGenreNamesByUserId(userId);
        return genres.size() > limit ? genres.subList(0, limit) : genres;
    }
    
    public double getGenreWeight(Long userId, String genre) {
        return tasteProfileRepository.findByUserUserIdAndGenre(userId, genre)
            .map(TasteProfile::getWeight)
            .orElse(1.0);
    }
}
