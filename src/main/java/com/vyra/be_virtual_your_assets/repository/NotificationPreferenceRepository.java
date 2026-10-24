package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationPreferenceRepository
        extends JpaRepository<NotificationPreference, NotificationPreference.Key> {

    List<NotificationPreference> findByMemberId(String memberId);
}
