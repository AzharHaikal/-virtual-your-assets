package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.category.CategoryResponse;
import com.vyra.be_virtual_your_assets.dto.category.CreateCategoryRequest;
import com.vyra.be_virtual_your_assets.dto.category.UpdateCategoryRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.vyra.be_virtual_your_assets.constant.ApiPath.CATEGORY_ID;

/** Built-in and the member's own transaction categories. */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_FINANCE_CATEGORIES)
public class FinanceCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public BaseResponse<List<CategoryResponse>> getCategories(
            @RequestParam(required = false) TransactionType type,
            Authentication authentication
    ) {
        return categoryService.getCategories(memberId(authentication), type);
    }

    @PostMapping
    public ResponseEntity<BaseResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(memberId(authentication), request));
    }

    @PatchMapping(CATEGORY_ID)
    public BaseResponse<CategoryResponse> updateCategory(
            @PathVariable String categoryId,
            @Valid @RequestBody UpdateCategoryRequest request,
            Authentication authentication
    ) {
        return categoryService.updateCategory(memberId(authentication), categoryId, request);
    }

    @DeleteMapping(CATEGORY_ID)
    public BaseResponse<Void> deleteCategory(@PathVariable String categoryId, Authentication authentication) {
        return categoryService.deleteCategory(memberId(authentication), categoryId);
    }

    private static String memberId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getMemberId();
    }
}
