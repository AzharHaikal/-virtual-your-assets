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
import com.vyra.be_virtual_your_assets.repository.BudgetRepository;
import com.vyra.be_virtual_your_assets.repository.FinanceCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final String MEMBER = "member-1";

    @Mock private FinanceCategoryRepository repository;
    @Mock private BudgetRepository budgetRepository;
    @Spy private TimeService timeService =
            new TimeService(Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta");

    @InjectMocks
    private CategoryService categoryService;

    // =========================================================================
    // list
    // =========================================================================
    @Test
    void getCategories_filtersByTypeAndFlagsCustomOnes() {
        when(repository.findVisible(MEMBER)).thenReturn(List.of(
                builtIn(TransactionCategory.SALARY, TransactionType.INCOME),
                builtIn(TransactionCategory.FOOD_AND_BEVERAGE, TransactionType.EXPENSE),
                custom("c-1", MEMBER, "Kopi", TransactionType.EXPENSE)));

        List<CategoryResponse> expenses = categoryService.getCategories(MEMBER, TransactionType.EXPENSE).getData();

        assertThat(expenses).extracting(CategoryResponse::getName).containsExactly("FOOD_AND_BEVERAGE", "Kopi");
        assertThat(expenses.get(0).isCustom()).isFalse();
        assertThat(expenses.get(0).getCode()).isEqualTo("FOOD_AND_BEVERAGE");
        assertThat(expenses.get(1).isCustom()).isTrue();
        assertThat(categoryService.getCategories(MEMBER, null).getData()).hasSize(3);
    }

    // =========================================================================
    // create / update / delete
    // =========================================================================
    @Test
    void createCategory_savesTrimmedNameUppercaseColorForTheMember() {
        when(repository.existsActiveName(MEMBER, TransactionType.EXPENSE, "Kopi", "")).thenReturn(false);

        BaseResponse<CategoryResponse> result = categoryService.createCategory(MEMBER, createRequest("  Kopi "));

        ArgumentCaptor<FinanceCategory> saved = ArgumentCaptor.forClass(FinanceCategory.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getMemberId()).isEqualTo(MEMBER);
        assertThat(saved.getValue().getName()).isEqualTo("Kopi");
        assertThat(saved.getValue().getColor()).isEqualTo("#A1B2C3");
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 22, 10, 0));
        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.CATEGORY_CREATE_SUCCESS.getCode());
        assertThat(result.getData().isCustom()).isTrue();
    }

    @Test
    void createCategory_duplicateName_isRejected() {
        when(repository.existsActiveName(MEMBER, TransactionType.EXPENSE, "Kopi", "")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> categoryService.createCategory(MEMBER, createRequest("Kopi")));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.CATEGORY_NAME_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void createCategory_overTheLimit_isRejected() {
        when(repository.countByMemberIdAndDeletedAtIsNull(MEMBER)).thenReturn((long) CategoryRules.MAX_CUSTOM);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> categoryService.createCategory(MEMBER, createRequest("Kopi")));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.CATEGORY_LIMIT_REACHED);
    }

    @Test
    void updateCategory_ownCategory_appliesChanges() {
        FinanceCategory own = custom("c-1", MEMBER, "Kopi", TransactionType.EXPENSE);
        when(repository.findByIdAndDeletedAtIsNull("c-1")).thenReturn(Optional.of(own));
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Kopi Pagi");
        request.setIcon("local_cafe");

        categoryService.updateCategory(MEMBER, "c-1", request);

        assertThat(own.getName()).isEqualTo("Kopi Pagi");
        assertThat(own.getIcon()).isEqualTo("local_cafe");
        assertThat(own.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateOrDelete_builtInOrOtherMembersCategory_is404() {
        when(repository.findByIdAndDeletedAtIsNull("default-SALARY"))
                .thenReturn(Optional.of(builtIn(TransactionCategory.SALARY, TransactionType.INCOME)));
        when(repository.findByIdAndDeletedAtIsNull("c-2"))
                .thenReturn(Optional.of(custom("c-2", "someone-else", "Hobi", TransactionType.EXPENSE)));

        assertThrows(NotFoundException.class,
                () -> categoryService.updateCategory(MEMBER, "default-SALARY", new UpdateCategoryRequest()));
        assertThrows(NotFoundException.class, () -> categoryService.deleteCategory(MEMBER, "c-2"));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteCategory_softDeletes() {
        FinanceCategory own = custom("c-1", MEMBER, "Kopi", TransactionType.EXPENSE);
        when(repository.findByIdAndDeletedAtIsNull("c-1")).thenReturn(Optional.of(own));

        categoryService.deleteCategory(MEMBER, "c-1");

        assertThat(own.getDeletedAt()).isEqualTo(LocalDateTime.of(2026, 10, 22, 10, 0));
        verify(repository).save(own);
    }

    // =========================================================================
    // resolveForTransaction
    // =========================================================================
    @Test
    void resolve_ownCustomCategoryById() {
        FinanceCategory own = custom("c-1", MEMBER, "Kopi", TransactionType.EXPENSE);
        when(repository.findByIdAndDeletedAtIsNull("c-1")).thenReturn(Optional.of(own));

        FinanceCategory result = categoryService.resolveForTransaction(MEMBER, TransactionType.EXPENSE, "c-1", null);

        assertThat(result).isSameAs(own);
        assertThat(result.legacyCode()).isEqualTo("OTHERS_EXPENSE");
    }

    @Test
    void resolve_legacyEnumAndDefault() {
        when(repository.findByIdAndDeletedAtIsNull("default-SALARY"))
                .thenReturn(Optional.of(builtIn(TransactionCategory.SALARY, TransactionType.INCOME)));
        when(repository.findByIdAndDeletedAtIsNull("default-OTHERS_EXPENSE"))
                .thenReturn(Optional.of(builtIn(TransactionCategory.OTHERS_EXPENSE, TransactionType.EXPENSE)));

        assertThat(categoryService.resolveForTransaction(MEMBER, TransactionType.INCOME, null, TransactionCategory.SALARY)
                .legacyCode()).isEqualTo("SALARY");
        assertThat(categoryService.resolveForTransaction(MEMBER, TransactionType.EXPENSE, null, null)
                .getId()).isEqualTo("default-OTHERS_EXPENSE");
    }

    @Test
    void resolve_typeMismatch_isRejected() {
        when(repository.findByIdAndDeletedAtIsNull("default-SALARY"))
                .thenReturn(Optional.of(builtIn(TransactionCategory.SALARY, TransactionType.INCOME)));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                categoryService.resolveForTransaction(MEMBER, TransactionType.EXPENSE, null, TransactionCategory.SALARY));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.CATEGORY_TYPE_MISMATCH);
    }

    @Test
    void resolve_otherMembersOrUnknownCategory_isRejected() {
        when(repository.findByIdAndDeletedAtIsNull("c-2"))
                .thenReturn(Optional.of(custom("c-2", "someone-else", "Hobi", TransactionType.EXPENSE)));
        when(repository.findByIdAndDeletedAtIsNull("nope")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> categoryService.resolveForTransaction(MEMBER, TransactionType.EXPENSE, "c-2", null));
        assertThrows(BadRequestException.class,
                () -> categoryService.resolveForTransaction(MEMBER, TransactionType.EXPENSE, "nope", null));
    }

    @Test
    void byIds_skipsNullsAndQueriesOnce() {
        FinanceCategory own = custom("c-1", MEMBER, "Kopi", TransactionType.EXPENSE);
        when(repository.findAllById(List.of("c-1"))).thenReturn(List.of(own));

        Map<String, FinanceCategory> result = categoryService.byIds(java.util.Arrays.asList("c-1", null, "c-1"));

        assertThat(result).containsOnlyKeys("c-1");
        assertThat(categoryService.byIds(List.of())).isEmpty();
    }

    @Test
    void byIds_empty_doesNotQuery() {
        categoryService.byIds(java.util.Collections.singletonList(null));
        verifyNoInteractions(repository);
    }

    // ------------------------------------------------------------------
    private static FinanceCategory builtIn(TransactionCategory code, TransactionType type) {
        FinanceCategory category = new FinanceCategory();
        category.setId(FinanceCategory.defaultId(code));
        category.setCode(code.name());
        category.setName(code.name());
        category.setType(type);
        category.setIcon("work");
        category.setColor("#22C55E");
        return category;
    }

    private static FinanceCategory custom(String id, String memberId, String name, TransactionType type) {
        FinanceCategory category = new FinanceCategory();
        category.setId(id);
        category.setMemberId(memberId);
        category.setName(name);
        category.setType(type);
        category.setIcon("local_cafe");
        category.setColor("#A1B2C3");
        return category;
    }

    private static CreateCategoryRequest createRequest(String name) {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName(name);
        request.setType(TransactionType.EXPENSE);
        request.setIcon("local_cafe");
        request.setColor("#a1b2c3");
        return request;
    }
}
