package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.goal.GoalResponse;
import com.vyra.be_virtual_your_assets.entity.Goal;
import com.vyra.be_virtual_your_assets.entity.GoalImage;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.GoalImageRepository;
import com.vyra.be_virtual_your_assets.repository.GoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalImageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 25, 3, 0);

    @Mock private GoalRepository goalRepository;
    @Mock private GoalImageRepository imageRepository;
    @Mock private GoalService goalService;

    private GoalImageService service;
    private final Goal goal = new Goal();

    @BeforeEach
    void setUp() {
        service = new GoalImageService(goalRepository, imageRepository, goalService,
                new TimeService(Clock.fixed(Instant.parse("2026-10-25T03:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta"));
        goal.setId("g-1");
    }

    @Test
    void save_storesTheImageAndStampsTheGoal() {
        when(goalRepository.findByIdAndMemberIdAndDeletedAtIsNull("g-1", "m-1")).thenReturn(Optional.of(goal));
        when(imageRepository.findById("g-1")).thenReturn(Optional.empty());
        when(goalService.getGoal("m-1", "g-1")).thenReturn(new BaseResponse<>("x", "x", new GoalResponse()));

        BaseResponse<GoalResponse> result = service.save("m-1", "g-1", PNG);

        ArgumentCaptor<GoalImage> saved = ArgumentCaptor.forClass(GoalImage.class);
        verify(imageRepository).save(saved.capture());
        assertThat(saved.getValue().getContentType()).isEqualTo("image/png");
        assertThat(saved.getValue().getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        assertThat(goal.getImageUpdatedAt()).isEqualTo(NOW);
        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.GOAL_IMAGE_SUCCESS.getCode());
    }

    @Test
    void save_overADeletedImage_makesItActiveAgain() {
        GoalImage deleted = new GoalImage();
        deleted.setGoalId("g-1");
        deleted.markDeleted(NOW.minusDays(1));
        when(goalRepository.findByIdAndMemberIdAndDeletedAtIsNull("g-1", "m-1")).thenReturn(Optional.of(goal));
        when(imageRepository.findById("g-1")).thenReturn(Optional.of(deleted));
        when(goalService.getGoal("m-1", "g-1")).thenReturn(new BaseResponse<>("x", "x", new GoalResponse()));

        service.save("m-1", "g-1", PNG);

        assertThat(deleted.getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        assertThat(deleted.getDeletedAt()).isNull();
        assertThat(deleted.getContent()).isEqualTo(PNG);
    }

    @Test
    void save_rejectsNonImagesAndTooLargeFiles() {
        when(goalRepository.findByIdAndMemberIdAndDeletedAtIsNull("g-1", "m-1")).thenReturn(Optional.of(goal));

        assertThrows(BadRequestException.class, () -> service.save("m-1", "g-1", "text".getBytes()));
        assertThrows(BadRequestException.class, () -> service.save("m-1", "g-1", new byte[AccountService.MAX_PHOTO_BYTES + 1]));
        assertThrows(BadRequestException.class, () -> service.save("m-1", "g-1", new byte[0]));
        verify(imageRepository, never()).save(any());
    }

    @Test
    void otherMembersGoal_isNotFound() {
        when(goalRepository.findByIdAndMemberIdAndDeletedAtIsNull("g-1", "m-2")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.get("m-2", "g-1"));
        assertThrows(NotFoundException.class, () -> service.save("m-2", "g-1", PNG));
    }

    @Test
    void delete_marksTheImageDeletedAndClearsTheStamp() {
        goal.setImageUpdatedAt(NOW);
        GoalImage image = new GoalImage();
        when(goalRepository.findByIdAndMemberIdAndDeletedAtIsNull("g-1", "m-1")).thenReturn(Optional.of(goal));
        when(imageRepository.findActive("g-1")).thenReturn(Optional.of(image));
        when(goalService.getGoal("m-1", "g-1")).thenReturn(new BaseResponse<>("x", "x", new GoalResponse()));

        assertThat(service.delete("m-1", "g-1").getResponseStatus())
                .isEqualTo(ErrorConstant.GOAL_IMAGE_DELETE_SUCCESS.getCode());
        verify(imageRepository).save(image);
        verify(imageRepository, never()).delete(any());
        assertThat(image.getStatus()).isEqualTo(ActiveStatus.DELETED);
        assertThat(image.getDeletedAt()).isEqualTo(NOW);
        assertThat(goal.getImageUpdatedAt()).isNull();
    }

    @Test
    void get_withoutImageOrDeletedOne_isNotFound() {
        when(goalRepository.findByIdAndMemberIdAndDeletedAtIsNull("g-1", "m-1")).thenReturn(Optional.of(goal));
        when(imageRepository.findActive("g-1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.get("m-1", "g-1"));
    }
}
