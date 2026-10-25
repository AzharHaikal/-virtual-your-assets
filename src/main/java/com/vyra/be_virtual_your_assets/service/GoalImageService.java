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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Cover image of the member's own goal: same rules as the profile photo (JPEG / PNG / WebP, max 1 MB).
 * Removing it is a soft delete (status DELETED); a new upload replaces the content and makes it ACTIVE.
 */
@Service
@RequiredArgsConstructor
public class GoalImageService {

    private final GoalRepository goalRepository;
    private final GoalImageRepository imageRepository;
    private final GoalService goalService;
    private final TimeService timeService;

    @Transactional
    public BaseResponse<GoalResponse> save(String memberId, String goalId, byte[] content) {
        Goal goal = findOwned(memberId, goalId);
        if (content == null || content.length == 0) {
            throw new BadRequestException(ErrorConstant.GOAL_IMAGE_INVALID);
        }
        if (content.length > AccountService.MAX_PHOTO_BYTES) {
            throw new BadRequestException(ErrorConstant.GOAL_IMAGE_TOO_LARGE);
        }
        String contentType = AccountService.imageType(content);
        if (contentType == null) {
            throw new BadRequestException(ErrorConstant.GOAL_IMAGE_INVALID);
        }
        LocalDateTime now = timeService.nowUtc();
        GoalImage image = imageRepository.findById(goal.getId()).orElseGet(() -> {
            GoalImage created = new GoalImage();
            created.setGoalId(goal.getId());
            return created;
        });
        image.setContent(content);
        image.setContentType(contentType);
        image.setUpdatedAt(now);
        image.setStatus(ActiveStatus.ACTIVE);
        image.setDeletedAt(null);
        imageRepository.save(image);
        goal.setImageUpdatedAt(now);
        goalRepository.saveAndFlush(goal);
        return withStatus(goalService.getGoal(memberId, goalId), ErrorConstant.GOAL_IMAGE_SUCCESS);
    }

    @Transactional(readOnly = true)
    public GoalImage get(String memberId, String goalId) {
        Goal goal = findOwned(memberId, goalId);
        return imageRepository.findActive(goal.getId())
                .orElseThrow(() -> new NotFoundException(ErrorConstant.GOAL_IMAGE_NOT_FOUND));
    }

    @Transactional
    public BaseResponse<GoalResponse> delete(String memberId, String goalId) {
        Goal goal = findOwned(memberId, goalId);
        LocalDateTime now = timeService.nowUtc();
        imageRepository.findActive(goal.getId()).ifPresent(image -> {
            image.markDeleted(now);
            imageRepository.save(image);
        });
        goal.setImageUpdatedAt(null);
        goalRepository.saveAndFlush(goal);
        return withStatus(goalService.getGoal(memberId, goalId), ErrorConstant.GOAL_IMAGE_DELETE_SUCCESS);
    }

    private Goal findOwned(String memberId, String goalId) {
        return goalRepository.findByIdAndMemberIdAndDeletedAtIsNull(goalId, memberId)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.GOAL_NOT_FOUND));
    }

    private static BaseResponse<GoalResponse> withStatus(BaseResponse<GoalResponse> detail, ErrorConstant status) {
        return new BaseResponse<>(status.getCode(), status.getMessage(), detail.getData());
    }
}
