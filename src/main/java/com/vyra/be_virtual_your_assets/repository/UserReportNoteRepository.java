package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.UserReportNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserReportNoteRepository extends JpaRepository<UserReportNote, String> {

    List<UserReportNote> findByReportIdOrderByCreatedAtAsc(String reportId);
}
