package com.raisetimeline.backend.repository;

import com.raisetimeline.backend.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowingId(Long followingId);

    long countByFollowerId(Long followerId);

    @Query("SELECT f FROM Follow f JOIN FETCH f.follower WHERE f.following.id = :followingId ORDER BY f.createdAt DESC")
    List<Follow> findFollowersByFollowingId(@Param("followingId") Long followingId);

    @Query("SELECT f FROM Follow f JOIN FETCH f.following WHERE f.follower.id = :followerId ORDER BY f.createdAt DESC")
    List<Follow> findFollowingByFollowerId(@Param("followerId") Long followerId);

    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :followerId AND f.following.id IN :userIds")
    Set<Long> findFollowingIdsByFollowerIdAndUserIds(@Param("followerId") Long followerId,
                                                     @Param("userIds") Collection<Long> userIds);
}
