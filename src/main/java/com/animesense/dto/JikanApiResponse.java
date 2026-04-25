package com.animesense.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

@Data
public class JikanApiResponse<T> {
    private T data;
    private Pagination pagination;
    
    @Data
    public static class Pagination {
        @SerializedName("last_visible_page")
        private int lastVisiblePage;
        
        @SerializedName("has_next_page")
        private boolean hasNextPage;
    }
}
