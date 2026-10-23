package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.mood.CreateMoodTagRequest;
import com.vyra.be_virtual_your_assets.dto.mood.MoodEntryRequest;
import com.vyra.be_virtual_your_assets.dto.mood.MoodEntryResponse;
import com.vyra.be_virtual_your_assets.dto.mood.MoodTagResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.MoodService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoodControllerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 23);

    @Mock private MoodService moodService;

    @InjectMocks
    private MoodController controller;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(new CustomUserDetails("member-1", "token"), null);

    @Test
    void entries_delegateWithTheAuthenticatedMember() {
        BaseResponse<List<MoodEntryResponse>> list = new BaseResponse<>("VYRA-MOD-LST-000", "ok", List.of());
        when(moodService.getEntries("member-1", DAY.minusDays(6), DAY)).thenReturn(list);
        assertThat(controller.getEntries(DAY.minusDays(6), DAY, authentication)).isSameAs(list);

        BaseResponse<MoodEntryResponse> today = new BaseResponse<>("VYRA-MOD-GET-000", "ok", null);
        when(moodService.getToday("member-1")).thenReturn(today);
        assertThat(controller.getToday(authentication)).isSameAs(today);

        MoodEntryRequest request = new MoodEntryRequest();
        BaseResponse<MoodEntryResponse> saved = new BaseResponse<>("VYRA-MOD-SAV-000", "ok", new MoodEntryResponse());
        when(moodService.saveEntry("member-1", DAY, request)).thenReturn(saved);
        assertThat(controller.saveEntry(DAY, request, authentication)).isSameAs(saved);

        BaseResponse<Void> deleted = new BaseResponse<>("VYRA-MOD-DEL-000", "ok", null);
        when(moodService.deleteEntry("member-1", DAY)).thenReturn(deleted);
        assertThat(controller.deleteEntry(DAY, authentication)).isSameAs(deleted);
    }

    @Test
    void tags_delegateWithTheAuthenticatedMember_andCreateIs201() {
        BaseResponse<List<MoodTagResponse>> list = new BaseResponse<>("VYRA-MTG-LST-000", "ok", List.of());
        when(moodService.getTags("member-1")).thenReturn(list);
        assertThat(controller.getTags(authentication)).isSameAs(list);

        CreateMoodTagRequest create = new CreateMoodTagRequest();
        BaseResponse<MoodTagResponse> created = new BaseResponse<>("VYRA-MTG-CRT-000", "ok", new MoodTagResponse());
        when(moodService.createTag("member-1", create)).thenReturn(created);
        ResponseEntity<BaseResponse<MoodTagResponse>> response = controller.createTag(create, authentication);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(created);

        BaseResponse<Void> deleted = new BaseResponse<>("VYRA-MTG-DEL-000", "ok", null);
        when(moodService.deleteTag("member-1", "t-1")).thenReturn(deleted);
        assertThat(controller.deleteTag("t-1", authentication)).isSameAs(deleted);
    }
}
