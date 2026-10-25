package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ConsentType;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.consent.ConsentResponse;
import com.vyra.be_virtual_your_assets.entity.MemberConsent;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.MemberConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The member's explicit opt-ins (Phase 3.5). Nothing that needs one runs without it. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentService {

    private final MemberConsentRepository consentRepository;
    private final ValidationService validationService;
    private final TimeService timeService;

    /** Every consent type with its state; never given = not granted. */
    @Transactional(readOnly = true)
    public BaseResponse<List<ConsentResponse>> getAll(String memberId) {
        Map<ConsentType, MemberConsent> given = consentRepository.findByMemberId(memberId).stream()
                .collect(Collectors.toMap(MemberConsent::getConsentType, Function.identity()));
        return new BaseResponse<>(ErrorConstant.CONSENT_LIST_SUCCESS.getCode(),
                ErrorConstant.CONSENT_LIST_SUCCESS.getMessage(),
                Arrays.stream(ConsentType.values()).map(type -> toResponse(type, given.get(type))).toList());
    }

    @Transactional
    public BaseResponse<ConsentResponse> set(String memberId, String type, boolean granted) {
        ConsentType consentType = ConsentType.parse(type);
        if (consentType == null) {
            throw new NotFoundException(ErrorConstant.CONSENT_TYPE_NOT_FOUND);
        }
        validationService.getMemberById(memberId);
        LocalDateTime now = timeService.nowUtc();
        MemberConsent consent = consentRepository.findByMemberIdAndConsentType(memberId, consentType)
                .orElseGet(() -> {
                    MemberConsent created = new MemberConsent();
                    created.setMemberId(memberId);
                    created.setConsentType(consentType);
                    return created;
                });
        if (granted && !consent.isGranted()) {
            consent.setGrantedAt(now);
            consent.setRevokedAt(null);
        } else if (!granted && consent.isGranted()) {
            consent.setRevokedAt(now);
        }
        consent.setUpdatedAt(now);
        consentRepository.save(consent);
        log.info("Consent saved. type: {} granted: {}", consentType, granted);
        return new BaseResponse<>(ErrorConstant.CONSENT_SAVED.getCode(), ErrorConstant.CONSENT_SAVED.getMessage(),
                toResponse(consentType, consent));
    }

    @Transactional(readOnly = true)
    public boolean isGranted(String memberId, ConsentType type) {
        return consentRepository.findByMemberIdAndConsentType(memberId, type).map(MemberConsent::isGranted).orElse(false);
    }

    private static ConsentResponse toResponse(ConsentType type, MemberConsent consent) {
        return consent == null ? new ConsentResponse(type, false, null, null)
                : new ConsentResponse(type, consent.isGranted(), consent.getGrantedAt(), consent.getRevokedAt());
    }
}
