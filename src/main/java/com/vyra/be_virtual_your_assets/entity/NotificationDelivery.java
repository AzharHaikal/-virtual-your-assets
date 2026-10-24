package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.DeliveryStatus;
import com.vyra.be_virtual_your_assets.constant.NotificationChannel;
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

import java.time.LocalDateTime;

/** Delivery of a notification outside the app; waits until {@link #deliverAfter} (e.g. the end of quiet hours). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "notification", name = "notification_delivery")
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String notificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private DeliveryStatus status;

    @Column(nullable = false)
    private LocalDateTime deliverAfter;

    @Column(nullable = false)
    private int attempts;

    private LocalDateTime sentAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
