package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.category.AdminCategoryResponse;
import com.vyra.be_virtual_your_assets.dto.category.CreateCategoryRequest;
import com.vyra.be_virtual_your_assets.dto.category.UpdateCategoryRequest;
import com.vyra.be_virtual_your_assets.entity.FinanceCategory;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.ConflictException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.FinanceCategoryRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCategoryServiceTest {

    @Mock private FinanceCategoryRepository categoryRepository;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @Mock private AdminActivityService adminActivityService;

    private AdminCategoryService service;
    private final CustomAdminDetails admin = new CustomAdminDetails("admin-1", "token", AdminRole.ADMINISTRATOR);

    @BeforeEach
    void setUp() {
        service = new AdminCategoryService(categoryRepository, adminAuthorizationService, adminActivityService,
                new TimeService(Clock.fixed(Instant.parse("2026-10-25T03:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta"));
    }

    @Test
    void create_addsBuiltInCategoryAtTheEnd() {
        when(categoryRepository.existsActiveBuiltInName(TransactionType.EXPENSE, "Pets", "")).thenReturn(false);
        when(categoryRepository.maxBuiltInPosition()).thenReturn(12);
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName(" Pets ");
        request.setType(TransactionType.EXPENSE);
        request.setIcon("pets");
        request.setColor("#ff8800");

        AdminCategoryResponse result = service.create(request, admin).getData();

        verify(adminAuthorizationService).requirePermission(AdminPermission.CATEGORY_MANAGE);
        ArgumentCaptor<FinanceCategory> saved = ArgumentCaptor.forClass(FinanceCategory.class);
        verify(categoryRepository).save(saved.capture());
        assertThat(saved.getValue().getMemberId()).isNull();
        assertThat(saved.getValue().getName()).isEqualTo("Pets");
        assertThat(saved.getValue().getColor()).isEqualTo("#FF8800");
        assertThat(saved.getValue().getPosition()).isEqualTo(13);
        assertThat(result.isActive()).isTrue();
        verify(adminActivityService).createAdminActivity(eq("admin-1"), isNull(),
                eq(AdminActivityEvent.CREATE_CATEGORY), eq("EXPENSE Pets"), isNull(), isNull());
    }

    @Test
    void create_duplicateName_isConflict() {
        when(categoryRepository.existsActiveBuiltInName(TransactionType.EXPENSE, "Food", "")).thenReturn(true);
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Food");
        request.setType(TransactionType.EXPENSE);
        request.setIcon("food");
        request.setColor("#000000");

        assertThrows(ConflictException.class, () -> service.create(request, admin));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_memberCategory_isNotFound() {
        FinanceCategory own = category("c-own", TransactionType.EXPENSE);
        own.setMemberId("m-1");
        when(categoryRepository.findById("c-own")).thenReturn(Optional.of(own));

        assertThrows(NotFoundException.class, () -> service.update("c-own", new UpdateCategoryRequest(), admin));
    }

    @Test
    void deactivate_hidesTheCategory_andIsIdempotent() {
        FinanceCategory category = category("c-1", TransactionType.EXPENSE);
        when(categoryRepository.findById("c-1")).thenReturn(Optional.of(category));

        assertThat(service.setActive("c-1", false, admin).getData().isActive()).isFalse();
        assertThat(category.getDeletedAt()).isNotNull();
        service.setActive("c-1", false, admin);

        verify(categoryRepository).save(category);
        verify(adminActivityService).createAdminActivity(eq("admin-1"), isNull(),
                eq(AdminActivityEvent.DEACTIVATE_CATEGORY), anyString(), isNull(), isNull());
    }

    @Test
    void deactivate_otherCategory_isProtected() {
        String otherId = FinanceCategory.otherId(TransactionType.EXPENSE);
        when(categoryRepository.findById(otherId)).thenReturn(Optional.of(category(otherId, TransactionType.EXPENSE)));

        assertThrows(BadRequestException.class, () -> service.setActive(otherId, false, admin));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void activate_checksTheNameAgainstActiveCategories() {
        FinanceCategory category = category("c-1", TransactionType.INCOME);
        category.setDeletedAt(java.time.LocalDateTime.of(2026, 10, 1, 0, 0));
        when(categoryRepository.findById("c-1")).thenReturn(Optional.of(category));
        when(categoryRepository.existsActiveBuiltInName(TransactionType.INCOME, "Bonus", "c-1")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.setActive("c-1", true, admin));
    }

    private static FinanceCategory category(String id, TransactionType type) {
        FinanceCategory category = new FinanceCategory();
        category.setId(id);
        category.setName("Bonus");
        category.setType(type);
        return category;
    }
}
