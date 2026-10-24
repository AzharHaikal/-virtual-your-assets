package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.constant.DataExportStatus;
import com.vyra.be_virtual_your_assets.entity.DataExport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DataExportRepository extends JpaRepository<DataExport, String> {

    Optional<DataExport> findByIdAndMemberId(String id, String memberId);

    Optional<DataExport> findTopByMemberIdOrderByRequestedAtDesc(String memberId);

    /** Ids only, so the job does not load files. */
    @Query("SELECT e.id FROM DataExport e WHERE e.status = :status ORDER BY e.requestedAt, e.id")
    List<String> findIdsByStatus(@Param("status") DataExportStatus status, Pageable pageable);

    /** An instance that died while building an export leaves it PROCESSING: put it back in the queue. */
    @Transactional
    @Modifying
    @Query("""
        UPDATE DataExport e SET e.status = com.vyra.be_virtual_your_assets.constant.DataExportStatus.PENDING
        WHERE e.status = com.vyra.be_virtual_your_assets.constant.DataExportStatus.PROCESSING
          AND e.startedAt < :before
        """)
    int requeueStuck(@Param("before") LocalDateTime before);

    /** Empties the files that may no longer be downloaded. */
    @Transactional
    @Modifying
    @Query("""
        UPDATE DataExport e
        SET e.status = com.vyra.be_virtual_your_assets.constant.DataExportStatus.EXPIRED,
            e.content = NULL, e.downloadTokenHash = NULL
        WHERE e.status = com.vyra.be_virtual_your_assets.constant.DataExportStatus.READY
          AND e.expiresAt < :now
        """)
    int expire(@Param("now") LocalDateTime now);
}
