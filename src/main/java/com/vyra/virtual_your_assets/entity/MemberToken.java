package com.vyra.virtual_your_assets.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(schema = "idp", name = "member_token")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String memberTokenId;

    private String memberId;

    @Column(length = 500)
    private String accessToken;

    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
}
