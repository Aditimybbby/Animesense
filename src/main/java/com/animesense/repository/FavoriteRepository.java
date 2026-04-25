package com.animesense.repository;

import com.animesense.model.Favorite;
import com.animesense.model.Favorite.FavoriteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    List<Favorite> findByUserUserId(Long userId);
    
    List<Favorite> findByUserUserIdAndStatus(Long userId, FavoriteStatus status);
    
    Optional<Favorite> findByUserUserIdAndAnimeAnimeId(Long userId, Long animeId);
    
    boolean existsByUserUserIdAndAnimeAnimeId(Long userId, Long animeId);
    
    @Query("SELECT f.anime.animeId FROM Favorite f WHERE f.user.userId = :userId")
    List<Long> findAnimeIdsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.user.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.user.userId = :userId AND f.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") FavoriteStatus status);
    
    @Query("SELECT AVG(f.userRating) FROM Favorite f WHERE f.user.userId = :userId AND f.userRating IS NOT NULL")
    Double findAverageRatingByUserId(@Param("userId") Long userId);
}
