package com.animesense.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

@Data
public class AnimeDTO {
    @SerializedName("mal_id")
    private Long malId;
    
    private String title;
    
    @SerializedName("title_english")
    private String titleEnglish;
    
    private String synopsis;
    
    private Double score;
    
    private Integer episodes;
    
    private String status;
    
    private Images images;
    
    private Trailer trailer;
    
    private Integer year;
    
    private List<Genre> genres;
    
    @Data
    public static class Images {
        private ImageFormat jpg;
        
        @Data
        public static class ImageFormat {
            @SerializedName("large_image_url")
            private String largeImageUrl;
        }
    }
    
    @Data
    public static class Trailer {
        private String url;
    }
    
    @Data
    public static class Genre {
        @SerializedName("mal_id")
        private Long malId;
        private String name;
    }
}
