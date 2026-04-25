package com.animesense.repository;

import com.animesense.model.Anime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnimeRepository extends JpaRepository<Anime, Long> {
    
    List<Anime> findByFeaturedTrue();
    
    @Query("SELECT a FROM Anime a WHERE a.rating >= :minRating ORDER BY a.rating DESC")
    List<Anime> findTopRatedAnime(@Param("minRating") double minRating);
    
    @Query("SELECT a FROM Anime a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(a.titleEnglish) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Anime> searchByTitle(@Param("query") String query);
    
    @Query("SELECT a FROM Anime a WHERE a.genres LIKE %:genre%")
    List<Anime> findByGenresContaining(@Param("genre") String genre);
    
    @Query("SELECT COUNT(a) FROM Anime a")
    long countAllAnime();
}
