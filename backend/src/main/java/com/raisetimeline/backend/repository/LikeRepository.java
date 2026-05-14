package com.raisetimeline.backend.repository;

import com.raisetimeline.backend.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    @Query("SELECT l.post.id FROM Like l WHERE l.user.id = :userId AND l.post.id IN :postIds")
    Set<Long> findLikedPostIdsByUserIdAndPostIds(@Param("userId") Long userId,
                                                  @Param("postIds") Collection<Long> postIds);

    @Query("SELECT l FROM Like l JOIN FETCH l.post WHERE l.user.id = :userId ORDER BY l.createdAt DESC")
    List<Like> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT l.post.id, COUNT(l) FROM Like l WHERE l.post.id IN :postIds GROUP BY l.post.id")
    List<Object[]> countByPostIds(@Param("postIds") Collection<Long> postIds);
}
