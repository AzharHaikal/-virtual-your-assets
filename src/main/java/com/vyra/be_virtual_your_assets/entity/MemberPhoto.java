package com.vyra.be_virtual_your_assets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Profile photo of a member (already compressed by the app); one row per member. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "idp", name = "member_photo")
public class MemberPhoto {

    @Id
    private String memberId;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false)
    private byte[] content;

    @Column(nullable = false, length = 20)
    private String contentType;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
