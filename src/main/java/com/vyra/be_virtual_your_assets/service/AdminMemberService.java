package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Member management for the portal: search/list, detail with financial summary,
 * transaction history, and status moderation (activate/suspend/delete).
 * Reads Member/Transaction/WalletStatement but never touches Admin tables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMemberService {

    private final MemberRepository memberRepository;
    private final TransactionRepository transactionRepository;
    private final WalletStatementRepository walletStatementRepository;
    private final AdminActivityService adminActivityService;
    private final AdminAuthorizationService adminAuthorizationService;
    private final MemberTokenRepository memberTokenRepository;

    public BaseResponse<PageResponse<AdminMemberListItemResponse>> getMemberList(String keyword, ActiveStatus status, int page, int size) {
        adminAuthorizationService.requirePermission(AdminPermission.MEMBER_VIEW);

        Page<Member> memberPage = memberRepository.searchMembers(
                StringUtils.isNotBlank(keyword) ? keyword.trim() : "",
                status,
                PageRequest.of(page, size)
        );

        boolean showBalance = adminAuthorizationService.canViewMemberTransactions();
        List<AdminMemberListItemResponse> items = memberPage.getContent()
                .stream()
                .map(member -> toListItemResponse(member, showBalance))
                .collect(Collectors.toList());

        return new BaseResponse<>(
                ErrorConstant.ADMIN_MEMBER_LIST_SUCCESS.getCode(),
                ErrorConstant.ADMIN_MEMBER_LIST_SUCCESS.getMessage(),
                PageResponse.from(memberPage, items)
        );
    }

    public BaseResponse<AdminMemberDetailResponse> getMemberDetail(String memberId) {
        adminAuthorizationService.requirePermission(AdminPermission.MEMBER_VIEW);

        Member member = getMemberOrThrow(memberId);
        WalletStatement walletStatement = walletStatementRepository.findByPhoneNumber(member.getPhoneNumber()).orElse(null);
        List<Transaction> recentTransactions = transactionRepository.findTransactionHistory(member.getId());

        AdminMemberDetailResponse response = new AdminMemberDetailResponse();
        response.setMemberId(member.getId());
        response.setFullName(member.getFirstName() + " " + member.getLastName());
        response.setEmail(member.getEmail());
        response.setPhoneNumber(member.getPhoneNumber());
        response.setStatus(member.getStatus());
        response.setJoinDate(member.getCreatedAt());

        // Balance and totals are member-level financial data: left null unless the admin may see them.
        if (adminAuthorizationService.canViewMemberTransactions()) {
            if (walletStatement != null) {
                response.setBalance(walletStatement.getBalance());
                response.setTotalIncome(walletStatement.getTotalIncome());
                response.setTotalExpense(walletStatement.getTotalExpense());
            } else {
                response.setBalance(BigDecimal.ZERO);
                response.setTotalIncome(BigDecimal.ZERO);
                response.setTotalExpense(BigDecimal.ZERO);
            }
        }

        response.setTotalTransactions(transactionRepository.findByMemberIdForAdmin(member.getId(), PageRequest.of(0, 1)).getTotalElements());
        response.setLastActivityAt(
                recentTransactions.isEmpty() ? null : recentTransactions.get(0).getTransactionDate()
        );

        return new BaseResponse<>(
                ErrorConstant.ADMIN_MEMBER_DETAIL_SUCCESS.getCode(),
                ErrorConstant.ADMIN_MEMBER_DETAIL_SUCCESS.getMessage(),
                response
        );
    }

    public BaseResponse<PageResponse<AdminMemberTransactionResponse>> getMemberTransactions(String memberId, int page, int size) {
        adminAuthorizationService.requireMemberTransactionAccess();

        Member member = getMemberOrThrow(memberId);
        CustomAdminDetails currentAdmin = adminAuthorizationService.getCurrentAdmin();
        adminActivityService.createAdminActivity(currentAdmin.getAdminId(), null,
                AdminActivityEvent.VIEW_MEMBER_TRANSACTIONS, "member " + member.getId(), null, null);
        Page<Transaction> transactionPage = transactionRepository.findByMemberIdForAdmin(member.getId(), PageRequest.of(page, size));

        List<AdminMemberTransactionResponse> items = transactionPage.getContent()
                .stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());

        return new BaseResponse<>(
                ErrorConstant.ADMIN_MEMBER_TRANSACTIONS_SUCCESS.getCode(),
                ErrorConstant.ADMIN_MEMBER_TRANSACTIONS_SUCCESS.getMessage(),
                PageResponse.from(transactionPage, items)
        );
    }

    @Transactional
    public BaseResponse<Void> updateMemberStatus(String memberId, AdminMemberStatusUpdateRequest request, CustomAdminDetails currentAdmin) {
        adminAuthorizationService.requirePermission(AdminPermission.MEMBER_MANAGE);

        Member member = getMemberOrThrow(memberId);
        member.setStatus(request.getStatus());
        if (request.getStatus() == ActiveStatus.ACTIVE) {
            // Re-activating also lifts temporary PIN / OTP locks.
            member.setFailedPinAttempts(0);
            member.setPinLockedUntil(null);
            member.setOtpLockedUntil(null);
        }
        member.setModifiedBy(currentAdmin.getAdminId());
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);

        if (request.getStatus() != ActiveStatus.ACTIVE) {
            // Suspended / inactive / deleted: end every session now instead of waiting for token expiry.
            memberTokenRepository.deleteByUserId(member.getId());
        }

        adminActivityService.createAdminActivity(
                currentAdmin.getAdminId(),
                currentAdmin.getAdminId(),
                AdminActivityEvent.UPDATE_MEMBER_STATUS,
                null,
                null
        );

        return new BaseResponse<>(
                ErrorConstant.ADMIN_MEMBER_STATUS_UPDATE_SUCCESS.getCode(),
                ErrorConstant.ADMIN_MEMBER_STATUS_UPDATE_SUCCESS.getMessage(),
                null
        );
    }

    @Transactional
    public BaseResponse<Void> deleteMember(String memberId, CustomAdminDetails currentAdmin) {
        adminAuthorizationService.requirePermission(AdminPermission.MEMBER_MANAGE);

        Member member = getMemberOrThrow(memberId);
        member.setDeletedAt(LocalDateTime.now());
        member.setStatus(ActiveStatus.DELETED);
        member.setModifiedBy(currentAdmin.getAdminId());
        memberRepository.save(member);
        memberTokenRepository.deleteByUserId(member.getId());

        adminActivityService.createAdminActivity(
                currentAdmin.getAdminId(),
                currentAdmin.getAdminId(),
                AdminActivityEvent.DELETE_MEMBER,
                null,
                null
        );

        return new BaseResponse<>(
                ErrorConstant.ADMIN_MEMBER_DELETE_SUCCESS.getCode(),
                ErrorConstant.ADMIN_MEMBER_DELETE_SUCCESS.getMessage(),
                null
        );
    }

    private Member getMemberOrThrow(String memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));
    }

    /** The balance is member-level financial data: left null unless {@code showBalance}. */
    private AdminMemberListItemResponse toListItemResponse(Member member, boolean showBalance) {
        AdminMemberListItemResponse response = new AdminMemberListItemResponse();
        response.setMemberId(member.getId());
        response.setFullName(member.getFirstName() + " " + member.getLastName());
        response.setEmail(member.getEmail());
        response.setPhoneNumber(member.getPhoneNumber());
        response.setStatus(member.getStatus());
        response.setJoinDate(member.getCreatedAt());

        if (showBalance) {
            walletStatementRepository.findByPhoneNumber(member.getPhoneNumber())
                    .ifPresentOrElse(
                            ws -> response.setBalance(ws.getBalance()),
                            () -> response.setBalance(BigDecimal.ZERO)
                    );
        }

        return response;
    }

    private AdminMemberTransactionResponse toTransactionResponse(Transaction transaction) {
        AdminMemberTransactionResponse response = new AdminMemberTransactionResponse();
        response.setTransactionId(transaction.getId());
        response.setReferenceNumber(transaction.getReferenceNumber());
        response.setCategory(transaction.getTransactionCategory());
        response.setType(transaction.getTransactionType());
        response.setStatus(transaction.getStatus());
        response.setAmount(transaction.getAmount());
        response.setDescription(transaction.getTransactionDesc());
        response.setTransactionDate(transaction.getTransactionDate());
        return response;
    }
}
