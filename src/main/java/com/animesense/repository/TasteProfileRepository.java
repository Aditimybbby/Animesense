package com.animesense.repository;

import com.animesense.model.TasteProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TasteProfileRepository extends JpaRepository<TasteProfile, Long> {
    
    Optional<TasteProfile> findByUserUserIdAndGenre(Long userId, String genre);
    
    List<TasteProfile> findByUserUserId(Long userId);
    
    @Query("SELECT tp FROM TasteProfile tp WHERE tp.user.userId = :userId ORDER BY tp.weight DESC")
    List<TasteProfile> findTopGenresByUserId(@Param("userId") Long userId);
    
    @Query("SELECT tp.genre FROM TasteProfile tp WHERE tp.user.userId = :userId ORDER BY tp.weight DESC")
    List<String> findTopGenreNamesByUserId(@Param("userId") Long userId);
}
