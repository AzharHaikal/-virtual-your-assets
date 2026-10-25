package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.AiReflection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AiReflectionRepository extends JpaRepository<AiReflection, String> {

    List<AiReflection> findTop20ByMemberIdAndDeletedAtIsNullOrderByPeriodStartDesc(String memberId);

    Optional<AiReflection> findByIdAndMemberIdAndDeletedAtIsNull(String id, String memberId);

    boolean existsByMemberIdAndPeriodStartAndDeletedAtIsNull(String memberId, LocalDate periodStart);
}
