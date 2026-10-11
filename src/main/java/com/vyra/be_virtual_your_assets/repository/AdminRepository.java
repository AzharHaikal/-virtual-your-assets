package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, String> {
    Optional<Admin> findByEmailIgnoreCase(String email);

    Optional<Admin> findByPhoneNumber(String phoneNumber);


    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query("""
        SELECT a
        FROM Admin a
        WHERE a.deletedAt IS NULL
        AND (
            :keyword IS NULL
            OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        ORDER BY a.createdAt DESC
        """)
    Page<Admin> searchAdmins(@Param("keyword") String keyword, Pageable pageable);
}

