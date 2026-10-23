package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.crypto.TextEncryptConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/** The member's mood of one local day (one active entry per day) with optional tags and journal. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "wellbeing", name = "mood_entry")
public class MoodEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String memberId;

    @Column(nullable = false)
    private LocalDate localDate;

    /** 1 (very bad) .. 5 (very good). */
    @Column(nullable = false, columnDefinition = "smallint")
    private short moodScore;

    /** Journal text, encrypted at rest. Never logged. */
    @Convert(converter = TextEncryptConverter.class)
    @Column(columnDefinition = "text")
    private String note;

    /** UTC instant of the (last) check-in. */
    @Column(nullable = false)
    private LocalDateTime checkedInAt;

    @ManyToMany
    @JoinTable(schema = "wellbeing", name = "mood_entry_tag",
            joinColumns = @JoinColumn(name = "mood_entry_id"),
            inverseJoinColumns = @JoinColumn(name = "mood_tag_id"))
    private Set<MoodTag> tags = new LinkedHashSet<>();
}
