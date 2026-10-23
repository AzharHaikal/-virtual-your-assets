package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.category.CategoryResponse;
import com.vyra.be_virtual_your_assets.dto.category.CreateCategoryRequest;
import com.vyra.be_virtual_your_assets.dto.category.UpdateCategoryRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceCategoryControllerTest {

    @Mock private CategoryService categoryService;

    @InjectMocks
    private FinanceCategoryController controller;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(new CustomUserDetails("member-1", "token"), null);

    @Test
    void list_create_update_delete_delegateWithTheAuthenticatedMember() {
        BaseResponse<List<CategoryResponse>> list = new BaseResponse<>("VYRA-CAT-LST-000", "ok", List.of());
        when(categoryService.getCategories("member-1", TransactionType.EXPENSE)).thenReturn(list);
        assertThat(controller.getCategories(TransactionType.EXPENSE, authentication)).isSameAs(list);

        CreateCategoryRequest create = new CreateCategoryRequest();
        BaseResponse<CategoryResponse> created = new BaseResponse<>("VYRA-CAT-CRT-000", "ok", new CategoryResponse());
        when(categoryService.createCategory("member-1", create)).thenReturn(created);
        ResponseEntity<BaseResponse<CategoryResponse>> response = controller.createCategory(create, authentication);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(created);

        UpdateCategoryRequest update = new UpdateCategoryRequest();
        BaseResponse<CategoryResponse> updated = new BaseResponse<>("VYRA-CAT-UPD-000", "ok", null);
        when(categoryService.updateCategory("member-1", "c-1", update)).thenReturn(updated);
        assertThat(controller.updateCategory("c-1", update, authentication)).isSameAs(updated);

        BaseResponse<Void> deleted = new BaseResponse<>("VYRA-CAT-DEL-000", "ok", null);
        when(categoryService.deleteCategory("member-1", "c-1")).thenReturn(deleted);
        assertThat(controller.deleteCategory("c-1", authentication)).isSameAs(deleted);
    }
}
