package com.vyra.virtual_your_assets.repository;

import com.vyra.virtual_your_assets.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findByPhoneNumber(String phoneNumber);
    Optional<Member> findByEmail(String email);

    @Query("""
        SELECT m FROM Member m
        WHERE m.email = :identifier OR m.phoneNumber = :identifier
    """)
    Optional<Member> findByIdentifier(@Param("identifier") String identifier);

    void deleteByPhoneNumber(String phoneNumber);
}
