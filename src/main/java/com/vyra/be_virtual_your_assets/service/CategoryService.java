package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.category.CategoryResponse;
import com.vyra.be_virtual_your_assets.dto.category.CategoryRules;
import com.vyra.be_virtual_your_assets.dto.category.CreateCategoryRequest;
import com.vyra.be_virtual_your_assets.dto.category.UpdateCategoryRequest;
import com.vyra.be_virtual_your_assets.entity.FinanceCategory;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.ConflictException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.FinanceCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transaction categories: the built-in ones (shared) plus each member's custom ones.
 * A member only ever sees, uses or changes built-in categories and their own.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final FinanceCategoryRepository categoryRepository;
    private final TimeService timeService;

    @Transactional(readOnly = true)
    public BaseResponse<List<CategoryResponse>> getCategories(String memberId, TransactionType type) {
        List<CategoryResponse> items = categoryRepository.findVisible(memberId).stream()
                .filter(c -> type == null || c.getType() == type)
                .map(CategoryService::toResponse)
                .collect(Collectors.toList());
        return new BaseResponse<>(ErrorConstant.CATEGORY_LIST_SUCCESS.getCode(),
                ErrorConstant.CATEGORY_LIST_SUCCESS.getMessage(), items);
    }

    @Transactional
    public BaseResponse<CategoryResponse> createCategory(String memberId, CreateCategoryRequest request) {
        if (categoryRepository.countByMemberIdAndDeletedAtIsNull(memberId) >= CategoryRules.MAX_CUSTOM) {
            throw new BadRequestException(ErrorConstant.CATEGORY_LIMIT_REACHED);
        }
        String name = request.getName().trim();
        if (categoryRepository.existsActiveName(memberId, request.getType(), name, "")) {
            throw new ConflictException(ErrorConstant.CATEGORY_NAME_EXISTS);
        }

        FinanceCategory category = new FinanceCategory();
        category.setMemberId(memberId);
        category.setName(name);
        category.setType(request.getType());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor().toUpperCase());
        category.setPosition(0);
        category.setCreatedBy(memberId);
        category.setCreatedAt(timeService.nowUtc());
        categoryRepository.save(category);

        return new BaseResponse<>(ErrorConstant.CATEGORY_CREATE_SUCCESS.getCode(),
                ErrorConstant.CATEGORY_CREATE_SUCCESS.getMessage(), toResponse(category));
    }

    @Transactional
    public BaseResponse<CategoryResponse> updateCategory(String memberId, String categoryId, UpdateCategoryRequest request) {
        FinanceCategory category = ownCategory(memberId, categoryId);

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new BadRequestException(ErrorConstant.BAD_REQUEST);
            }
            if (categoryRepository.existsActiveName(memberId, category.getType(), name, category.getId())) {
                throw new ConflictException(ErrorConstant.CATEGORY_NAME_EXISTS);
            }
            category.setName(name);
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }
        if (request.getColor() != null) {
            category.setColor(request.getColor().toUpperCase());
        }
        category.setModifiedBy(memberId);
        category.setUpdatedAt(timeService.nowUtc());
        categoryRepository.save(category);

        return new BaseResponse<>(ErrorConstant.CATEGORY_UPDATE_SUCCESS.getCode(),
                ErrorConstant.CATEGORY_UPDATE_SUCCESS.getMessage(), toResponse(category));
    }

    /** Soft delete: transactions already using it keep showing its name. */
    @Transactional
    public BaseResponse<Void> deleteCategory(String memberId, String categoryId) {
        FinanceCategory category = ownCategory(memberId, categoryId);
        category.setDeletedAt(timeService.nowUtc());
        category.setModifiedBy(memberId);
        categoryRepository.save(category);
        return new BaseResponse<>(ErrorConstant.CATEGORY_DELETE_SUCCESS.getCode(),
                ErrorConstant.CATEGORY_DELETE_SUCCESS.getMessage(), null);
    }

    /**
     * Category for a new or edited transaction of {@code type}: {@code categoryId} (built-in or the member's own,
     * not deleted), else the legacy enum, else "Other" of the type. The category must match the type.
     */
    @Transactional(readOnly = true)
    public FinanceCategory resolveForTransaction(String memberId, TransactionType type, String categoryId,
                                                 TransactionCategory legacyCategory) {
        String id = categoryId != null ? categoryId
                : legacyCategory != null ? FinanceCategory.defaultId(legacyCategory)
                : FinanceCategory.otherId(type);

        FinanceCategory category = categoryRepository.findByIdAndDeletedAtIsNull(id)
                .filter(c -> c.isBuiltIn() || Objects.equals(c.getMemberId(), memberId))
                .orElseThrow(() -> new BadRequestException(ErrorConstant.CATEGORY_NOT_FOUND));
        if (category.getType() != type) {
            throw new BadRequestException(ErrorConstant.CATEGORY_TYPE_MISMATCH);
        }
        return category;
    }

    /** Categories by id, deleted ones included (for showing existing transactions). */
    @Transactional(readOnly = true)
    public Map<String, FinanceCategory> byIds(Collection<String> ids) {
        List<String> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return categoryRepository.findAllById(distinct).stream()
                .collect(Collectors.toMap(FinanceCategory::getId, Function.identity()));
    }

    private FinanceCategory ownCategory(String memberId, String categoryId) {
        // Built-in categories and other members' categories look like missing ones.
        return categoryRepository.findByIdAndDeletedAtIsNull(categoryId)
                .filter(c -> Objects.equals(c.getMemberId(), memberId))
                .orElseThrow(() -> new NotFoundException(ErrorConstant.CATEGORY_NOT_FOUND));
    }

    static CategoryResponse toResponse(FinanceCategory category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setCode(category.getCode());
        response.setName(category.getName());
        response.setType(category.getType());
        response.setIcon(category.getIcon());
        response.setColor(category.getColor());
        response.setCustom(!category.isBuiltIn());
        return response;
    }
}
