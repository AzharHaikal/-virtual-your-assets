package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActivationEvent;
import com.vyra.be_virtual_your_assets.repository.MemberActivationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Activation metrics: the first time a member did each core action (PRD §13). Called by the services
 * of those actions; repeating an action changes nothing.
 */
@Service
@RequiredArgsConstructor
public class ActivationService {

    private final MemberActivationRepository activationRepository;
    private final TimeService timeService;

    /** Joins the caller's transaction, so a rolled back action is not counted. */
    @Transactional
    public void record(String memberId, ActivationEvent event) {
        activationRepository.insertIfAbsent(memberId, event.name(), timeService.nowUtc());
    }
}
