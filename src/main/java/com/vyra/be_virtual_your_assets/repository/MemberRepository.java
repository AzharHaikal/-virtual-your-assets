package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {
    Optional<Member> findByPhoneNumber(String phoneNumber);

    Optional<Member> findByEmailIgnoreCase(String email);


    long countByStatus(ActiveStatus status);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Member> findTop5ByOrderByCreatedAtDesc();

    List<Member> findByIdIn(java.util.Collection<String> ids);

    @Query("""
        SELECT m
        FROM Member m
        WHERE m.deletedAt IS NULL
        AND (:status IS NULL OR m.status = :status)
        AND (
            :keyword IS NULL
            OR LOWER(m.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR m.phoneNumber LIKE CONCAT('%', :keyword, '%')
        )
        ORDER BY m.createdAt DESC
        """)
    Page<Member> searchMembers(
            @Param("keyword") String keyword,
            @Param("status") ActiveStatus status,
            Pageable pageable
    );
}

