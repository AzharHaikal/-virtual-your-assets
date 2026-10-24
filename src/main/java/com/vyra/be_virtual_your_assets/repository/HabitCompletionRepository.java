package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.HabitCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HabitCompletionRepository extends JpaRepository<HabitCompletion, String> {

    /** Sets the progress of the day; atomic, so parallel taps on the same day never create two rows. */
    @Modifying
    @Query(value = """
        INSERT INTO wellbeing.habit_completion (id, habit_id, member_id, local_date, count, completed_at)
        VALUES (:id, :habitId, :memberId, :localDate, :count, :completedAt)
        ON CONFLICT (habit_id, local_date)
        DO UPDATE SET count = EXCLUDED.count, completed_at = EXCLUDED.completed_at
        """, nativeQuery = true)
    int upsert(@Param("id") String id,
               @Param("habitId") String habitId,
               @Param("memberId") String memberId,
               @Param("localDate") LocalDate localDate,
               @Param("count") int count,
               @Param("completedAt") LocalDateTime completedAt);

    @Modifying
    @Query("DELETE FROM HabitCompletion c WHERE c.habitId = :habitId AND c.localDate = :localDate")
    int deleteDay(@Param("habitId") String habitId, @Param("localDate") LocalDate localDate);

    @Query("""
        SELECT c FROM HabitCompletion c
        WHERE c.memberId = :memberId AND c.localDate BETWEEN :from AND :to
        """)
    List<HabitCompletion> findRange(@Param("memberId") String memberId,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);

    /** Days in [from, to] each of the member's habits reached its target. Rows: [habitId, localDate]. */
    @Query("""
        SELECT c.habitId, c.localDate FROM HabitCompletion c, Habit h
        WHERE h.id = c.habitId AND c.memberId = :memberId
          AND c.localDate BETWEEN :from AND :to
          AND c.count >= h.targetCount
        """)
    List<Object[]> findDoneDays(@Param("memberId") String memberId,
                                @Param("from") LocalDate from,
                                @Param("to") LocalDate to);

    List<HabitCompletion> findByHabitIdAndLocalDateBetween(String habitId, LocalDate from, LocalDate to);

    Optional<HabitCompletion> findByHabitIdAndLocalDate(String habitId, LocalDate localDate);
}
