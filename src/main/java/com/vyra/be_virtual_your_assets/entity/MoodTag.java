package com.vyra.be_virtual_your_assets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A mood tag ("what made you feel this way"): built-in ({@link #memberId} null) or the member's own. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "wellbeing", name = "mood_tag")
public class MoodTag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String memberId;

    @Column(length = 50)
    private String code;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false)
    private int position;

    public boolean isBuiltIn() {
        return memberId == null;
    }
}
