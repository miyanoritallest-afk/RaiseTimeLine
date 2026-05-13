package com.raisetimeline.backend.repository;

import com.raisetimeline.backend.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p JOIN FETCH p.user ORDER BY p.id DESC")
    List<Post> findAllForFeed(Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.id < :cursor ORDER BY p.id DESC")
    List<Post> findAllForFeedBefore(@Param("cursor") Long cursor, Pageable pageable);

    @Query("""
        SELECT p FROM Post p JOIN FETCH p.user
        WHERE p.user.id IN (
            SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId
        )
        ORDER BY p.id DESC
        """)
    List<Post> findFollowingFeed(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT p FROM Post p JOIN FETCH p.user
        WHERE p.user.id IN (
            SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId
        )
        AND p.id < :cursor
        ORDER BY p.id DESC
        """)
    List<Post> findFollowingFeedBefore(@Param("userId") Long userId,
                                       @Param("cursor") Long cursor,
                                       Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    List<Post> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
