package com.animesense.service;

import com.animesense.model.Anime;
import com.animesense.repository.AnimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
        try {
            if (anime == null || anime.getAnimeId() == null) {
                log.warn("Attempted to add or update null anime or anime without ID");
                return null;
            }

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
        } catch (Exception e) {
            log.error("Error adding or updating anime with ID: {}", 
                    anime != null ? anime.getAnimeId() : "null", e);
            return null;
        }
    }
    
    @Transactional
    public Anime fetchAndSaveAnime(Long animeId) {
        try {
            if (animeId == null || animeId <= 0) {
                log.warn("Invalid anime ID provided: {}", animeId);
                return null;
            }

            Anime anime = jikanApiService.getAnimeById(animeId);
            if (anime != null) {
                return addOrUpdateAnime(anime);
            }
            log.warn("No anime found from API for ID: {}", animeId);
            return null;
        } catch (Exception e) {
            log.error("Error fetching and saving anime with ID: {}", animeId, e);
            return null;
        }
    }
    
    public Optional<Anime> getAnimeById(Long animeId) {
        try {
            if (animeId == null || animeId <= 0) {
                log.warn("Invalid anime ID provided: {}", animeId);
                return Optional.empty();
            }
            return animeRepository.findById(animeId);
        } catch (Exception e) {
            log.error("Error retrieving anime with ID: {}", animeId, e);
            return Optional.empty();
        }
    }
    
    public List<Anime> getAllAnime() {
        try {
            return animeRepository.findAll();
        } catch (Exception e) {
            log.error("Error retrieving all anime", e);
            return new ArrayList<>();
        }
    }
    
    public List<Anime> getFeaturedAnime() {
        try {
            return animeRepository.findByFeaturedTrue();
        } catch (Exception e) {
            log.error("Error retrieving featured anime", e);
            return new ArrayList<>();
        }
    }
    
    public List<Anime> searchAnime(String query) {
        try {
            if (query == null || query.isBlank()) {
                log.warn("Search attempted with empty query");
                return new ArrayList<>();
            }

            List<Anime> results = animeRepository.searchByTitle(query);
            
            if (results.size() < 5) {
                try {
                    List<Anime> apiResults = jikanApiService.searchAnime(query, 10);
                    if (apiResults != null) {
                        for (Anime anime : apiResults) {
                            addOrUpdateAnime(anime);
                        }
                        results = animeRepository.searchByTitle(query);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch additional results from API for query: {}", query, e);
                }
            }
            
            return results;
        } catch (Exception e) {
            log.error("Error searching anime with query: {}", query, e);
            return new ArrayList<>();
        }
    }
    
    @Transactional
    public void setFeatured(Long animeId, boolean featured) {
        try {
            if (animeId == null || animeId <= 0) {
                log.warn("Invalid anime ID for setting featured: {}", animeId);
                return;
            }

            animeRepository.findById(animeId).ifPresentOrElse(
                anime -> {
                    anime.setFeatured(featured);
                    animeRepository.save(anime);
                    log.info("Set featured status to {} for anime ID: {}", featured, animeId);
                },
                () -> log.warn("Anime not found with ID: {}", animeId)
            );
        } catch (Exception e) {
            log.error("Error setting featured status for anime ID: {}", animeId, e);
        }
    }
    
    @Transactional
    public void setAdminBoost(Long animeId, double boost) {
        try {
            if (animeId == null || animeId <= 0) {
                log.warn("Invalid anime ID for setting admin boost: {}", animeId);
                return;
            }

            if (boost < 0) {
                log.warn("Invalid boost value: {}. Must be non-negative", boost);
                return;
            }

            animeRepository.findById(animeId).ifPresentOrElse(
                anime -> {
                    anime.setAdminBoost(boost);
                    animeRepository.save(anime);
                    log.info("Set admin boost to {} for anime ID: {}", boost, animeId);
                },
                () -> log.warn("Anime not found with ID: {}", animeId)
            );
        } catch (Exception e) {
            log.error("Error setting admin boost for anime ID: {}", animeId, e);
        }
    }
    
    @Transactional
    public void deleteAnime(Long animeId) {
        try {
            if (animeId == null || animeId <= 0) {
                log.warn("Invalid anime ID for deletion: {}", animeId);
                return;
            }

            if (animeRepository.existsById(animeId)) {
                animeRepository.deleteById(animeId);
                log.info("Successfully deleted anime with ID: {}", animeId);
            } else {
                log.warn("Attempted to delete non-existent anime with ID: {}", animeId);
            }
        } catch (Exception e) {
            log.error("Error deleting anime with ID: {}", animeId, e);
        }
    }
    
    public long getTotalAnimeCount() {
        try {
            return animeRepository.count();
        } catch (Exception e) {
            log.error("Error retrieving total anime count", e);
            return 0;
        }
    }
}