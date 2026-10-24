package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface HabitRepository extends JpaRepository<Habit, String> {

    Optional<Habit> findByIdAndMemberIdAndDeletedAtIsNull(String id, String memberId);

    /** The member's habits, not deleted, oldest first (the order they were added). */
    @Query("""
        SELECT h FROM Habit h
        WHERE h.memberId = :memberId AND h.deletedAt IS NULL
        ORDER BY h.createdAt, h.id
        """)
    List<Habit> findAllOfMember(@Param("memberId") String memberId);

    long countByMemberIdAndDeletedAtIsNull(String memberId);

    /** Time zones of active members with at least one active habit reminder. */
    @Query("""
        SELECT DISTINCT m.timezone FROM Habit h, Member m
        WHERE m.id = h.memberId
          AND m.status = com.vyra.be_virtual_your_assets.constant.ActiveStatus.ACTIVE
          AND h.reminderEnabled = true AND h.archivedAt IS NULL AND h.deletedAt IS NULL
        """)
    List<String> findReminderZones();

    /**
     * Active reminders of active members in {@code zone} whose local time is in (from, to].
     * Rows: [Habit, Member].
     */
    @Query("""
        SELECT h, m FROM Habit h, Member m
        WHERE m.id = h.memberId AND m.timezone = :zone
          AND m.status = com.vyra.be_virtual_your_assets.constant.ActiveStatus.ACTIVE
          AND h.reminderEnabled = true AND h.archivedAt IS NULL AND h.deletedAt IS NULL
          AND h.reminderTime > :from AND h.reminderTime <= :to
        """)
    List<Object[]> findReminderCandidates(@Param("zone") String zone,
                                          @Param("from") LocalTime from,
                                          @Param("to") LocalTime to);

    /** As {@link #findReminderCandidates} for [00:00, to]: the first minutes of the day. */
    @Query("""
        SELECT h, m FROM Habit h, Member m
        WHERE m.id = h.memberId AND m.timezone = :zone
          AND m.status = com.vyra.be_virtual_your_assets.constant.ActiveStatus.ACTIVE
          AND h.reminderEnabled = true AND h.archivedAt IS NULL AND h.deletedAt IS NULL
          AND h.reminderTime <= :to
        """)
    List<Object[]> findReminderCandidatesUpTo(@Param("zone") String zone, @Param("to") LocalTime to);
}
