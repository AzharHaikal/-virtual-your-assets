package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.category.AdminCategoryResponse;
import com.vyra.be_virtual_your_assets.dto.category.CreateCategoryRequest;
import com.vyra.be_virtual_your_assets.dto.category.UpdateCategoryRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import com.vyra.be_virtual_your_assets.service.AdminCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Built-in transaction categories (SETTINGS_VIEW to read, CATEGORY_MANAGE to change). */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_ADMIN_CATEGORIES)
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    public BaseResponse<List<AdminCategoryResponse>> getCategories() {
        return adminCategoryService.getCategories();
    }

    @PostMapping
    public ResponseEntity<BaseResponse<AdminCategoryResponse>> create(
            @Valid @RequestBody CreateCategoryRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCategoryService.create(request, admin(authentication)));
    }

    @PatchMapping(ApiPath.ADMIN_CATEGORY_ID)
    public BaseResponse<AdminCategoryResponse> update(
            @PathVariable String categoryId,
            @Valid @RequestBody UpdateCategoryRequest request,
            Authentication authentication
    ) {
        return adminCategoryService.update(categoryId, request, admin(authentication));
    }

    @PostMapping(ApiPath.ADMIN_CATEGORY_ACTIVATE)
    public BaseResponse<AdminCategoryResponse> activate(@PathVariable String categoryId, Authentication authentication) {
        return adminCategoryService.setActive(categoryId, true, admin(authentication));
    }

    @PostMapping(ApiPath.ADMIN_CATEGORY_DEACTIVATE)
    public BaseResponse<AdminCategoryResponse> deactivate(@PathVariable String categoryId, Authentication authentication) {
        return adminCategoryService.setActive(categoryId, false, admin(authentication));
    }

    private static CustomAdminDetails admin(Authentication authentication) {
        return (CustomAdminDetails) authentication.getPrincipal();
    }
}
