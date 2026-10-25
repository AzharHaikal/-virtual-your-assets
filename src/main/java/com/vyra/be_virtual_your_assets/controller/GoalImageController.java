package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.goal.GoalResponse;
import com.vyra.be_virtual_your_assets.entity.GoalImage;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.AccountService;
import com.vyra.be_virtual_your_assets.service.GoalImageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

/** Cover image of the member's own goal; the body of PUT is the raw image (like the profile photo). */
@RestController
@RequiredArgsConstructor
public class GoalImageController {

    private final GoalImageService goalImageService;

    /** At most 1 MB + 1 byte is read, so a huge upload never fills the memory. */
    @PutMapping(ApiPath.V1_GOALS + ApiPath.GOAL_IMAGE)
    public BaseResponse<GoalResponse> save(@PathVariable String goalId, HttpServletRequest request,
                                           Authentication authentication) throws IOException {
        byte[] content;
        try (InputStream body = request.getInputStream()) {
            content = body.readNBytes(AccountService.MAX_PHOTO_BYTES + 1);
        }
        return goalImageService.save(memberId(authentication), goalId, content);
    }

    @GetMapping(ApiPath.V1_GOALS + ApiPath.GOAL_IMAGE)
    public ResponseEntity<byte[]> get(@PathVariable String goalId, Authentication authentication) {
        GoalImage image = goalImageService.get(memberId(authentication), goalId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(image.getContent());
    }

    @DeleteMapping(ApiPath.V1_GOALS + ApiPath.GOAL_IMAGE)
    public BaseResponse<GoalResponse> delete(@PathVariable String goalId, Authentication authentication) {
        return goalImageService.delete(memberId(authentication), goalId);
    }

    private static String memberId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getMemberId();
    }
}
