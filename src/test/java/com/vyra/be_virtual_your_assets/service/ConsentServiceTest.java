package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ConsentType;
import com.vyra.be_virtual_your_assets.dto.consent.ConsentResponse;
import com.vyra.be_virtual_your_assets.entity.MemberConsent;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.MemberConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 25, 3, 0);

    @Mock private MemberConsentRepository consentRepository;
    @Mock private ValidationService validationService;

    private ConsentService service;

    @BeforeEach
    void setUp() {
        service = new ConsentService(consentRepository, validationService, new TimeService(Clock.fixed(Instant.parse("2026-10-25T03:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta"));
    }

    @Test
    void getAll_neverGiven_isNotGranted() {
        when(consentRepository.findByMemberId("m-1")).thenReturn(List.of());

        List<ConsentResponse> result = service.getAll("m-1").getData();

        assertThat(result).singleElement().satisfies(c -> {
            assertThat(c.getType()).isEqualTo(ConsentType.AI_REFLECTION);
            assertThat(c.isGranted()).isFalse();
        });
    }

    @Test
    void grantThenRevoke_keepsTheRowWithBothTimes() {
        when(consentRepository.findByMemberIdAndConsentType("m-1", ConsentType.AI_REFLECTION)).thenReturn(Optional.empty());

        ConsentResponse granted = service.set("m-1", "ai_reflection", true).getData();

        assertThat(granted.isGranted()).isTrue();
        assertThat(granted.getGrantedAt()).isEqualTo(NOW);

        MemberConsent stored = new MemberConsent();
        stored.setMemberId("m-1");
        stored.setConsentType(ConsentType.AI_REFLECTION);
        stored.setGrantedAt(NOW.minusDays(3));
        when(consentRepository.findByMemberIdAndConsentType("m-1", ConsentType.AI_REFLECTION)).thenReturn(Optional.of(stored));

        ConsentResponse revoked = service.set("m-1", "AI_REFLECTION", false).getData();

        assertThat(revoked.isGranted()).isFalse();
        assertThat(revoked.getRevokedAt()).isEqualTo(NOW);
        assertThat(revoked.getGrantedAt()).isEqualTo(NOW.minusDays(3));
        verify(consentRepository).save(stored);
        assertThat(service.isGranted("m-1", ConsentType.AI_REFLECTION)).isFalse();
    }

    @Test
    void unknownType_isNotFound() {
        assertThrows(NotFoundException.class, () -> service.set("m-1", "MARKETING", true));
    }
}
