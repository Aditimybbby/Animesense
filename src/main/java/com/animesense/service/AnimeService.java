package com.animesense.service;

import com.animesense.model.Anime;
import com.animesense.repository.AnimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnimeService {
    
    private final AnimeRepository animeRepository;
    private final JikanApiService jikanApiService;
    
    @Transactional
    public Anime addOrUpdateAnime(Anime anime) {
        Optional<Anime> existing = animeRepository.findById(anime.getAnimeId());
        
        if (existing.isPresent()) {
            Anime existingAnime = existing.get();
            existingAnime.setTitle(anime.getTitle());
            existingAnime.setTitleEnglish(anime.getTitleEnglish());
            existingAnime.setSynopsis(anime.getSynopsis());
            existingAnime.setGenres(anime.getGenres());
            existingAnime.setRating(anime.getRating());
            existingAnime.setEpisodes(anime.getEpisodes());
            existingAnime.setStatus(anime.getStatus());
            existingAnime.setImageUrl(anime.getImageUrl());
            existingAnime.setTrailerUrl(anime.getTrailerUrl());
            existingAnime.setYear(anime.getYear());
            return animeRepository.save(existingAnime);
        }
        
        return animeRepository.save(anime);
    }
    
    @Transactional
    public Anime fetchAndSaveAnime(Long animeId) {
        Anime anime = jikanApiService.getAnimeById(animeId);
        if (anime != null) {
            return addOrUpdateAnime(anime);
        }
        return null;
    }
    
    public Optional<Anime> getAnimeById(Long animeId) {
        return animeRepository.findById(animeId);
    }
    
    public List<Anime> getAllAnime() {
        return animeRepository.findAll();
    }
    
    public List<Anime> getFeaturedAnime() {
        return animeRepository.findByFeaturedTrue();
    }
    
    public List<Anime> searchAnime(String query) {
        // First search in database
        List<Anime> results = animeRepository.searchByTitle(query);
        
        // If not enough results, search API
        if (results.size() < 5) {
            List<Anime> apiResults = jikanApiService.searchAnime(query, 10);
            for (Anime anime : apiResults) {
                addOrUpdateAnime(anime);
            }
            results = animeRepository.searchByTitle(query);
        }
        
        return results;
    }
    
    @Transactional
    public void setFeatured(Long animeId, boolean featured) {
        animeRepository.findById(animeId).ifPresent(anime -> {
            anime.setFeatured(featured);
            animeRepository.save(anime);
        });
    }
    
    @Transactional
    public void setAdminBoost(Long animeId, double boost) {
        animeRepository.findById(animeId).ifPresent(anime -> {
            anime.setAdminBoost(boost);
            animeRepository.save(anime);
        });
    }
    
    @Transactional
    public void deleteAnime(Long animeId) {
        animeRepository.deleteById(animeId);
    }
    
    public long getTotalAnimeCount() {
        return animeRepository.count();
    }
}
