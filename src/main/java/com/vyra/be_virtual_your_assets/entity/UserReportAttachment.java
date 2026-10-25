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

/** Screenshot of a report; its own table so loading reports never reads the image. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "idp", name = "user_report_attachment")
public class UserReportAttachment {

    @Id
    private String reportId;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false)
    private byte[] content;

    @Column(nullable = false, length = 20)
    private String contentType;
}
