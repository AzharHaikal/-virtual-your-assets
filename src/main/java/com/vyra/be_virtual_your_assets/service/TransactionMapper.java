package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.dto.transaction.TransactionHistoryResponse;
import com.vyra.be_virtual_your_assets.entity.FinanceCategory;
import com.vyra.be_virtual_your_assets.entity.Transaction;

import java.util.Map;

/** Transaction -> member-facing response, with its category (looked up in {@code categories} by id). */
final class TransactionMapper {

    private TransactionMapper() {
    }

    static TransactionHistoryResponse toHistoryResponse(Transaction transaction, Map<String, FinanceCategory> categories) {
        TransactionHistoryResponse response = new TransactionHistoryResponse();
        response.setTransactionId(transaction.getId());
        response.setReferenceNumber(transaction.getReferenceNumber());
        response.setCategory(transaction.getTransactionCategory());
        FinanceCategory category = categories.get(transaction.getCategoryId());
        if (category != null) {
            response.setCategoryId(category.getId());
            response.setCategoryName(category.getName());
            response.setCategoryIcon(category.getIcon());
            response.setCategoryColor(category.getColor());
        }
        response.setType(transaction.getTransactionType());
        response.setStatus(transaction.getStatus());
        response.setAmount(transaction.getAmount());
        response.setTransactionDesc(transaction.getTransactionDesc());
        response.setPaymentMethod(transaction.getPaymentMethod());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setLocalDate(transaction.getLocalDate());
        return response;
    }
}
