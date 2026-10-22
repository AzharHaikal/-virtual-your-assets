package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.*;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.adminmember.AdminMemberDetailResponse;
import com.vyra.be_virtual_your_assets.dto.adminmember.AdminMemberListItemResponse;
import com.vyra.be_virtual_your_assets.dto.adminmember.AdminMemberStatusUpdateRequest;
import com.vyra.be_virtual_your_assets.dto.adminmember.AdminMemberTransactionResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import com.vyra.be_virtual_your_assets.entity.WalletStatement;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.MemberTokenRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMemberServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletStatementRepository walletStatementRepository;
    @Mock private AdminActivityService adminActivityService;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @Mock private MemberTokenRepository memberTokenRepository;

    @InjectMocks
    private AdminMemberService adminMemberService;

    private Member member;
    private CustomAdminDetails currentAdmin;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setId("member-id-001");
        member.setFirstName("Budi");
        member.setLastName("Santoso");
        member.setEmail("budi@example.com");
        member.setPhoneNumber("628123456789");
        member.setStatus(ActiveStatus.ACTIVE);
        member.setCreatedAt(LocalDateTime.now());

        currentAdmin = new CustomAdminDetails("admin-id-001", "token", AdminRole.OPERATOR);
    }

    // =========================================================================
    // getMemberList
    // =========================================================================
    @Test
    void getMemberList_shouldReturnPagedMembersWithBalance() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.MEMBER_VIEW);
        Page<Member> page = new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1);
        when(memberRepository.searchMembers(eq("budi"), eq(ActiveStatus.ACTIVE), any(PageRequest.class))).thenReturn(page);

        WalletStatement ws = new WalletStatement();
        ws.setBalance(new BigDecimal("500000"));
        when(walletStatementRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.of(ws));

        BaseResponse<PageResponse<AdminMemberListItemResponse>> result =
                adminMemberService.getMemberList("budi", ActiveStatus.ACTIVE, 0, 10);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_MEMBER_LIST_SUCCESS.getCode());
        assertThat(result.getData().getContent()).hasSize(1);
        assertThat(result.getData().getContent().get(0).getBalance()).isEqualTo(new BigDecimal("500000"));
    }

    @Test
    void getMemberList_memberWithoutWalletStatement_shouldDefaultBalanceToZero() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.MEMBER_VIEW);
        Page<Member> page = new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1);
        when(memberRepository.searchMembers(any(), any(), any(PageRequest.class))).thenReturn(page);
        when(walletStatementRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.empty());

        BaseResponse<PageResponse<AdminMemberListItemResponse>> result =
                adminMemberService.getMemberList(null, null, 0, 10);

        assertThat(result.getData().getContent().get(0).getBalance()).isEqualTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // getMemberDetail
    // =========================================================================
    @Test
    void getMemberDetail_withWalletStatement_shouldReturnFullFinancialSummary() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.MEMBER_VIEW);
        when(memberRepository.findById("member-id-001")).thenReturn(Optional.of(member));

        WalletStatement ws = new WalletStatement();
        ws.setBalance(new BigDecimal("750000"));
        ws.setTotalIncome(new BigDecimal("1000000"));
        ws.setTotalExpense(new BigDecimal("250000"));
        when(walletStatementRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.of(ws));

        Transaction latestTransaction = buildTransaction(LocalDateTime.now());
        when(transactionRepository.findTransactionHistory("member-id-001")).thenReturn(List.of(latestTransaction));

        Page<Transaction> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 7);
        when(transactionRepository.findByMemberIdForAdmin(eq("member-id-001"), any(PageRequest.class))).thenReturn(emptyPage);

        BaseResponse<AdminMemberDetailResponse> result = adminMemberService.getMemberDetail("member-id-001");

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_MEMBER_DETAIL_SUCCESS.getCode());
        AdminMemberDetailResponse data = result.getData();
        assertThat(data.getFullName()).isEqualTo("Budi Santoso");
        assertThat(data.getBalance()).isEqualTo(new BigDecimal("750000"));
        assertThat(data.getTotalIncome()).isEqualTo(new BigDecimal("1000000"));
        assertThat(data.getTotalTransactions()).isEqualTo(7L);
        assertThat(data.getLastActivityAt()).isEqualTo(latestTransaction.getTransactionDate());
    }

    @Test
    void getMemberDetail_withoutWalletStatement_shouldDefaultFinancialFieldsToZero() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.MEMBER_VIEW);
        when(memberRepository.findById("member-id-001")).thenReturn(Optional.of(member));
        when(walletStatementRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.empty());
        when(transactionRepository.findTransactionHistory("member-id-001")).thenReturn(List.of());

        Page<Transaction> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 0);
        when(transactionRepository.findByMemberIdForAdmin(eq("member-id-001"), any(PageRequest.class))).thenReturn(emptyPage);

        BaseResponse<AdminMemberDetailResponse> result = adminMemberService.getMemberDetail("member-id-001");

        assertThat(result.getData().getBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getData().getTotalIncome()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getData().getTotalExpense()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getData().getLastActivityAt()).isNull();
    }

    @Test
    void getMemberDetail_memberNotFound_shouldThrowBusinessException() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.MEMBER_VIEW);
        when(memberRepository.findById("unknown")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminMemberService.getMemberDetail("unknown"));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_FOUND);
    }

    // =========================================================================
    // getMemberTransactions
    // =========================================================================
    @Test
    void getMemberTransactions_shouldReturnPagedTransactionResponses() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.MEMBER_VIEW);
        when(memberRepository.findById("member-id-001")).thenReturn(Optional.of(member));

        Transaction transaction = buildTransaction(LocalDateTime.now());
        Page<Transaction> page = new PageImpl<>(List.of(transaction), PageRequest.of(0, 10), 1);
        when(transactionRepository.findByMemberIdForAdmin(eq("member-id-001"), any(PageRequest.class))).thenReturn(page);

        BaseResponse<PageResponse<AdminMemberTransactionResponse>> result =
                adminMemberService.getMemberTransactions("member-id-001", 0, 10);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_MEMBER_TRANSACTIONS_SUCCESS.getCode());
        assertThat(result.getData().getContent()).hasSize(1);
        assertThat(result.getData().getContent().get(0).getCategory()).isEqualTo("SALARY");
    }

    @Test
    void getMemberTransactions_memberNotFound_shouldThrowBusinessException() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.MEMBER_VIEW);
        when(memberRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> adminMemberService.getMemberTransactions("unknown", 0, 10));
    }

    // =========================================================================
    // updateMemberStatus
    // =========================================================================
    @Test
    void updateMemberStatus_success_shouldUpdateStatusAndLogActivity() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.MEMBER_MANAGE);
        when(memberRepository.findById("member-id-001")).thenReturn(Optional.of(member));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());

        AdminMemberStatusUpdateRequest request = new AdminMemberStatusUpdateRequest();
        request.setStatus(ActiveStatus.SUSPENDED);

        BaseResponse<Void> result = adminMemberService.updateMemberStatus("member-id-001", request, currentAdmin);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_MEMBER_STATUS_UPDATE_SUCCESS.getCode());
        assertThat(member.getStatus()).isEqualTo(ActiveStatus.SUSPENDED);
        assertThat(member.getModifiedBy()).isEqualTo("admin-id-001");
        verify(adminActivityService).createAdminActivity(eq("admin-id-001"), anyString(), eq(AdminActivityEvent.UPDATE_MEMBER_STATUS), any(), any());
        // The member is thrown out immediately, not when the token happens to expire.
        verify(memberTokenRepository).deleteByUserId("member-id-001");
    }

    @Test
    void updateMemberStatus_reactivate_shouldNotTouchSessions() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.MEMBER_MANAGE);
        when(memberRepository.findById("member-id-001")).thenReturn(Optional.of(member));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminMemberStatusUpdateRequest request = new AdminMemberStatusUpdateRequest();
        request.setStatus(ActiveStatus.ACTIVE);

        adminMemberService.updateMemberStatus("member-id-001", request, currentAdmin);

        verify(memberTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void updateMemberStatus_memberNotFound_shouldThrowBusinessException() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.MEMBER_MANAGE);
        when(memberRepository.findById("unknown")).thenReturn(Optional.empty());

        AdminMemberStatusUpdateRequest request = new AdminMemberStatusUpdateRequest();
        request.setStatus(ActiveStatus.SUSPENDED);

        assertThrows(BusinessException.class,
                () -> adminMemberService.updateMemberStatus("unknown", request, currentAdmin));
        verify(memberRepository, never()).save(any());
    }

    // =========================================================================
    // deleteMember
    // =========================================================================
    @Test
    void deleteMember_success_shouldSoftDeleteAndLogActivity() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.MEMBER_MANAGE);
        when(memberRepository.findById("member-id-001")).thenReturn(Optional.of(member));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());

        BaseResponse<Void> result = adminMemberService.deleteMember("member-id-001", currentAdmin);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_MEMBER_DELETE_SUCCESS.getCode());
        assertThat(member.getStatus()).isEqualTo(ActiveStatus.DELETED);
        assertThat(member.getDeletedAt()).isNotNull();
        verify(adminActivityService).createAdminActivity(eq("admin-id-001"), anyString(), eq(AdminActivityEvent.DELETE_MEMBER), any(), any());
        verify(memberTokenRepository).deleteByUserId("member-id-001");
    }

    @Test
    void deleteMember_memberNotFound_shouldThrowBusinessException() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.MEMBER_MANAGE);
        when(memberRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> adminMemberService.deleteMember("unknown", currentAdmin));
        verify(memberRepository, never()).save(any());
    }

    // =========================================================================
    // helpers
    // =========================================================================
    private Transaction buildTransaction(LocalDateTime transactionDate) {
        Transaction transaction = new Transaction();
        transaction.setId("trx-001");
        transaction.setMemberId("member-id-001");
        transaction.setReferenceNumber("REF-001");
        transaction.setTransactionCategory("SALARY");
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setAmount(new BigDecimal("1000000"));
        transaction.setTransactionDesc("Monthly salary");
        transaction.setTransactionDate(transactionDate);
        return transaction;
    }
}
