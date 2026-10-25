package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.constant.UserReportStatus;
import com.vyra.be_virtual_your_assets.entity.UserReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserReportRepository extends JpaRepository<UserReport, String> {

    List<UserReport> findTop50ByMemberIdOrderByCreatedAtDesc(String memberId);

    long countByMemberIdAndCreatedAtAfter(String memberId, LocalDateTime after);

    Page<UserReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<UserReport> findByStatusOrderByCreatedAtDesc(UserReportStatus status, Pageable pageable);

    long countByStatus(UserReportStatus status);
}
