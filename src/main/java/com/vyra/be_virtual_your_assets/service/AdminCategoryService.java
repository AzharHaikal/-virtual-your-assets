package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.category.AdminCategoryResponse;
import com.vyra.be_virtual_your_assets.dto.category.CreateCategoryRequest;
import com.vyra.be_virtual_your_assets.dto.category.UpdateCategoryRequest;
import com.vyra.be_virtual_your_assets.entity.FinanceCategory;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.ConflictException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.FinanceCategoryRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Built-in transaction categories (member_id NULL), shared by every member. Deactivating one hides it from
 * new transactions and budgets; transactions that already use it keep showing it. The two "Other"
 * categories are the fallback of every transaction and stay active.
 */
@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final FinanceCategoryRepository categoryRepository;
    private final AdminAuthorizationService adminAuthorizationService;
    private final AdminActivityService adminActivityService;
    private final TimeService timeService;

    @Transactional(readOnly = true)
    public BaseResponse<List<AdminCategoryResponse>> getCategories() {
        adminAuthorizationService.requirePermission(AdminPermission.SETTINGS_VIEW);
        return new BaseResponse<>(ErrorConstant.ADMIN_CATEGORY_LIST_SUCCESS.getCode(),
                ErrorConstant.ADMIN_CATEGORY_LIST_SUCCESS.getMessage(),
                categoryRepository.findAllBuiltIn().stream().map(AdminCategoryService::toResponse).toList());
    }

    @Transactional
    public BaseResponse<AdminCategoryResponse> create(CreateCategoryRequest request, CustomAdminDetails admin) {
        adminAuthorizationService.requirePermission(AdminPermission.CATEGORY_MANAGE);
        String name = request.getName().trim();
        checkName(request.getType(), name, "");
        LocalDateTime now = timeService.nowUtc();
        FinanceCategory category = new FinanceCategory();
        category.setName(name);
        category.setType(request.getType());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor().toUpperCase(Locale.ROOT));
        category.setPosition(categoryRepository.maxBuiltInPosition() + 1);
        category.setCreatedAt(now);
        category.setCreatedBy(admin.getAdminId());
        categoryRepository.save(category);
        audit(admin, AdminActivityEvent.CREATE_CATEGORY, category);
        return saved(category);
    }

    @Transactional
    public BaseResponse<AdminCategoryResponse> update(String categoryId, UpdateCategoryRequest request,
                                                     CustomAdminDetails admin) {
        adminAuthorizationService.requirePermission(AdminPermission.CATEGORY_MANAGE);
        FinanceCategory category = findBuiltIn(categoryId);
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (category.getDeletedAt() == null) {
                checkName(category.getType(), name, category.getId());
            }
            category.setName(name);
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }
        if (request.getColor() != null) {
            category.setColor(request.getColor().toUpperCase(Locale.ROOT));
        }
        touch(category, admin);
        categoryRepository.save(category);
        audit(admin, AdminActivityEvent.UPDATE_CATEGORY, category);
        return saved(category);
    }

    /** Idempotent both ways. */
    @Transactional
    public BaseResponse<AdminCategoryResponse> setActive(String categoryId, boolean active, CustomAdminDetails admin) {
        adminAuthorizationService.requirePermission(AdminPermission.CATEGORY_MANAGE);
        FinanceCategory category = findBuiltIn(categoryId);
        boolean isActive = category.getDeletedAt() == null;
        if (active != isActive) {
            if (!active && isFallback(category)) {
                throw new BadRequestException(ErrorConstant.ADMIN_CATEGORY_PROTECTED);
            }
            if (active) {
                checkName(category.getType(), category.getName(), category.getId());
            }
            category.setDeletedAt(active ? null : timeService.nowUtc());
            touch(category, admin);
            categoryRepository.save(category);
            audit(admin, active ? AdminActivityEvent.ACTIVATE_CATEGORY : AdminActivityEvent.DEACTIVATE_CATEGORY, category);
        }
        return saved(category);
    }

    static boolean isFallback(FinanceCategory category) {
        return category.getId().equals(FinanceCategory.otherId(TransactionType.INCOME))
                || category.getId().equals(FinanceCategory.otherId(TransactionType.EXPENSE));
    }

    private void checkName(TransactionType type, String name, String excludeId) {
        if (categoryRepository.existsActiveBuiltInName(type, name, excludeId)) {
            throw new ConflictException(ErrorConstant.CATEGORY_NAME_EXISTS);
        }
    }

    private FinanceCategory findBuiltIn(String categoryId) {
        return categoryRepository.findById(categoryId)
                .filter(FinanceCategory::isBuiltIn)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.CATEGORY_NOT_FOUND));
    }

    private void touch(FinanceCategory category, CustomAdminDetails admin) {
        category.setUpdatedAt(timeService.nowUtc());
        category.setModifiedBy(admin.getAdminId());
    }

    private void audit(CustomAdminDetails admin, AdminActivityEvent event, FinanceCategory category) {
        adminActivityService.createAdminActivity(admin.getAdminId(), null, event,
                category.getType() + " " + category.getName(), null, null);
    }

    private static BaseResponse<AdminCategoryResponse> saved(FinanceCategory category) {
        return new BaseResponse<>(ErrorConstant.ADMIN_CATEGORY_SAVE_SUCCESS.getCode(),
                ErrorConstant.ADMIN_CATEGORY_SAVE_SUCCESS.getMessage(), toResponse(category));
    }

    static AdminCategoryResponse toResponse(FinanceCategory category) {
        AdminCategoryResponse response = new AdminCategoryResponse();
        response.setId(category.getId());
        response.setCode(category.getCode());
        response.setName(category.getName());
        response.setType(category.getType());
        response.setIcon(category.getIcon());
        response.setColor(category.getColor());
        response.setPosition(category.getPosition());
        response.setActive(category.getDeletedAt() == null);
        response.setUpdatedAt(category.getUpdatedAt() != null ? category.getUpdatedAt() : category.getCreatedAt());
        return response;
    }
}
