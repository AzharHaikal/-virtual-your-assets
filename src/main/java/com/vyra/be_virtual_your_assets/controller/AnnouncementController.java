package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.announcement.AnnouncementRequest;
import com.vyra.be_virtual_your_assets.dto.announcement.AnnouncementResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Announcements: the member's active banners, and their management in the portal. */
@RestController
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping(ApiPath.V1_ANNOUNCEMENTS)
    public BaseResponse<List<AnnouncementResponse>> getActive(Authentication authentication) {
        return announcementService.getActive(((CustomUserDetails) authentication.getPrincipal()).getMemberId());
    }

    @GetMapping(ApiPath.V1_ADMIN_ANNOUNCEMENTS)
    public BaseResponse<PageResponse<AnnouncementResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return announcementService.getAll(page, size);
    }

    @PostMapping(ApiPath.V1_ADMIN_ANNOUNCEMENTS)
    public ResponseEntity<BaseResponse<AnnouncementResponse>> create(
            @Valid @RequestBody AnnouncementRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(announcementService.create(request, admin(authentication)));
    }

    @PutMapping(ApiPath.V1_ADMIN_ANNOUNCEMENTS + ApiPath.ADMIN_ANNOUNCEMENT_ID)
    public BaseResponse<AnnouncementResponse> update(
            @PathVariable String announcementId,
            @Valid @RequestBody AnnouncementRequest request,
            Authentication authentication
    ) {
        return announcementService.update(announcementId, request, admin(authentication));
    }

    @PostMapping(ApiPath.V1_ADMIN_ANNOUNCEMENTS + ApiPath.ADMIN_ANNOUNCEMENT_PUBLISH)
    public BaseResponse<AnnouncementResponse> publish(@PathVariable String announcementId, Authentication authentication) {
        return announcementService.publish(announcementId, admin(authentication));
    }

    @PostMapping(ApiPath.V1_ADMIN_ANNOUNCEMENTS + ApiPath.ADMIN_ANNOUNCEMENT_UNPUBLISH)
    public BaseResponse<AnnouncementResponse> unpublish(@PathVariable String announcementId, Authentication authentication) {
        return announcementService.unpublish(announcementId, admin(authentication));
    }

    @DeleteMapping(ApiPath.V1_ADMIN_ANNOUNCEMENTS + ApiPath.ADMIN_ANNOUNCEMENT_ID)
    public BaseResponse<Void> delete(@PathVariable String announcementId, Authentication authentication) {
        return announcementService.delete(announcementId, admin(authentication));
    }

    private static CustomAdminDetails admin(Authentication authentication) {
        return (CustomAdminDetails) authentication.getPrincipal();
    }
}
