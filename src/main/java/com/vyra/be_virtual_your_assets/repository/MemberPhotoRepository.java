package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.MemberPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MemberPhotoRepository extends JpaRepository<MemberPhoto, String> {

    /** When the photo last changed (null without a photo), without reading the image. */
    @Query("SELECT p.updatedAt FROM MemberPhoto p WHERE p.memberId = :memberId")
    Optional<LocalDateTime> findUpdatedAt(@Param("memberId") String memberId);
}
