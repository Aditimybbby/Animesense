package com.animesense.service;

import com.animesense.dto.AnimeDTO;
import com.animesense.dto.JikanApiResponse;
import com.animesense.model.Anime;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JikanApiService {
    
    @Value("${jikan.api.base-url}")
    private String baseUrl;
    
    @Value("${jikan.api.rate-limit-ms}")
    private long rateLimitMs;
    
    private final OkHttpClient client;
    private final Gson gson;
    private long lastRequestTime = 0;
    
    private static final Map<String, Integer> GENRE_MAP = new HashMap<>();
    
    static {
        GENRE_MAP.put("Action", 1);
        GENRE_MAP.put("Adventure", 2);
        GENRE_MAP.put("Comedy", 4);
        GENRE_MAP.put("Drama", 8);
        GENRE_MAP.put("Fantasy", 10);
        GENRE_MAP.put("Horror", 14);
        GENRE_MAP.put("Mystery", 7);
        GENRE_MAP.put("Romance", 22);
        GENRE_MAP.put("Sci-Fi", 24);
        GENRE_MAP.put("Slice of Life", 36);
        GENRE_MAP.put("Sports", 30);
        GENRE_MAP.put("Supernatural", 37);
        GENRE_MAP.put("Thriller", 41);
        GENRE_MAP.put("Psychological", 40);
        GENRE_MAP.put("Mecha", 18);
    }
    
    public JikanApiService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }
    
    private void rateLimit() {
        long elapsed = System.currentTimeMillis() - lastRequestTime;
        if (elapsed < rateLimitMs) {
            try {
                Thread.sleep(rateLimitMs - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }
    
    public Anime getAnimeById(Long animeId) {
        rateLimit();
        
        try {
            String url = baseUrl + "/anime/" + animeId;
            Request request = new Request.Builder().url(url).build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Failed to fetch anime {}: {}", animeId, response.code());
                    return null;
                }
                
                String responseBody = response.body().string();
                Type type = new TypeToken<JikanApiResponse<AnimeDTO>>(){}.getType();
                JikanApiResponse<AnimeDTO> apiResponse = gson.fromJson(responseBody, type);
                
                return convertToAnime(apiResponse.getData());
            }
        } catch (IOException e) {
            log.error("Error fetching anime {}: {}", animeId, e.getMessage());
            return null;
        }
    }
    
    public List<Anime> searchAnime(String query, int limit) {
        rateLimit();
        
        try {
            String url = String.format("%s/anime?q=%s&limit=%d&order_by=score&sort=desc", 
                                      baseUrl, query, limit);
            Request request = new Request.Builder().url(url).build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Failed to search anime: {}", response.code());
                    return new ArrayList<>();
                }
                
                String responseBody = response.body().string();
                Type type = new TypeToken<JikanApiResponse<List<AnimeDTO>>>(){}.getType();
                JikanApiResponse<List<AnimeDTO>> apiResponse = gson.fromJson(responseBody, type);
                
                return apiResponse.getData().stream()
                    .map(this::convertToAnime)
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("Error searching anime: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public List<Anime> getTopAnime(int limit) {
        rateLimit();
        
        try {
            String url = String.format("%s/top/anime?limit=%d", baseUrl, limit);
            Request request = new Request.Builder().url(url).build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Failed to fetch top anime: {}", response.code());
                    return new ArrayList<>();
                }
                
                String responseBody = response.body().string();
                Type type = new TypeToken<JikanApiResponse<List<AnimeDTO>>>(){}.getType();
                JikanApiResponse<List<AnimeDTO>> apiResponse = gson.fromJson(responseBody, type);
                
                return apiResponse.getData().stream()
                    .map(this::convertToAnime)
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("Error fetching top anime: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public List<Anime> getAnimeByGenres(List<String> genres, int limit) {
        rateLimit();
        
        String genreIds = genres.stream()
            .map(GENRE_MAP::get)
            .filter(id -> id != null)
            .map(String::valueOf)
            .collect(Collectors.joining(","));
        
        if (genreIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String url = String.format("%s/anime?genres=%s&limit=%d&order_by=score&sort=desc", 
                                      baseUrl, genreIds, limit);
            Request request = new Request.Builder().url(url).build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Failed to fetch anime by genres: {}", response.code());
                    return new ArrayList<>();
                }
                
                String responseBody = response.body().string();
                Type type = new TypeToken<JikanApiResponse<List<AnimeDTO>>>(){}.getType();
                JikanApiResponse<List<AnimeDTO>> apiResponse = gson.fromJson(responseBody, type);
                
                return apiResponse.getData().stream()
                    .map(this::convertToAnime)
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("Error fetching anime by genres: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private Anime convertToAnime(AnimeDTO dto) {
        Anime anime = new Anime();
        anime.setAnimeId(dto.getMalId());
        anime.setTitle(dto.getTitle());
        anime.setTitleEnglish(dto.getTitleEnglish());
        anime.setSynopsis(dto.getSynopsis());
        anime.setRating(dto.getScore());
        anime.setEpisodes(dto.getEpisodes());
        anime.setStatus(dto.getStatus());
        anime.setYear(dto.getYear());
        
        if (dto.getImages() != null && dto.getImages().getJpg() != null) {
            anime.setImageUrl(dto.getImages().getJpg().getLargeImageUrl());
        }
        
        if (dto.getTrailer() != null) {
            anime.setTrailerUrl(dto.getTrailer().getUrl());
        }
        
        if (dto.getGenres() != null) {
            String genres = dto.getGenres().stream()
                .map(AnimeDTO.Genre::getName)
                .collect(Collectors.joining(","));
            anime.setGenres(genres);
        }
        
        return anime;
    }
    
    public static List<String> getAvailableGenres() {
        return new ArrayList<>(GENRE_MAP.keySet());
    }
}
