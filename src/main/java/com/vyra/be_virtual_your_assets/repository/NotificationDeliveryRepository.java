package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.constant.DeliveryStatus;
import com.vyra.be_virtual_your_assets.entity.NotificationDelivery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, String> {

    /** Deliveries whose time has come, oldest first. */
    @Query("""
        SELECT d FROM NotificationDelivery d
        WHERE d.status = :status AND d.deliverAfter <= :now
        ORDER BY d.deliverAfter, d.id
        """)
    List<NotificationDelivery> findDue(@Param("status") DeliveryStatus status,
                                       @Param("now") LocalDateTime now,
                                       Pageable pageable);
}
