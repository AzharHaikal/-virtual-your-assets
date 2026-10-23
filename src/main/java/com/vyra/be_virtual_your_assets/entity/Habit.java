package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.HabitCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** A habit of a member with its weekly schedule (bit 0 = Monday .. bit 6 = Sunday). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "wellbeing", name = "habit")
public class Habit extends BaseEntity {

    public static final short EVERY_DAY = 0b111_1111;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String memberId;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HabitCategory category;

    @Column(nullable = false, length = 40)
    private String icon;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(nullable = false)
    private int targetCount = 1;

    @Column(nullable = false)
    private short scheduleDays = EVERY_DAY;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDateTime archivedAt;

    public boolean isArchived() {
        return archivedAt != null;
    }

    /** Scheduled on that day of the week and already started. */
    public boolean isScheduledOn(LocalDate day) {
        return !day.isBefore(startDate) && (scheduleDays & bit(day.getDayOfWeek())) != 0;
    }

    public List<DayOfWeek> scheduledDays() {
        return Arrays.stream(DayOfWeek.values()).filter(d -> (scheduleDays & bit(d)) != 0).toList();
    }

    public static short mask(Collection<DayOfWeek> days) {
        int mask = 0;
        for (DayOfWeek day : days) {
            mask |= bit(day);
        }
        return (short) mask;
    }

    private static int bit(DayOfWeek day) {
        return 1 << (day.getValue() - 1);
    }
}
