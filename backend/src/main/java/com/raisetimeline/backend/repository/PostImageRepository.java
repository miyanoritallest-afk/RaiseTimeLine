package com.raisetimeline.backend.repository;

import com.raisetimeline.backend.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
}
