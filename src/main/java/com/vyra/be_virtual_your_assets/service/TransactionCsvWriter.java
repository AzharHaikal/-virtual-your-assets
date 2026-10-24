package com.vyra.be_virtual_your_assets.service;

import com.opencsv.CSVWriter;
import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.entity.FinanceCategory;
import com.vyra.be_virtual_your_assets.entity.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * The member's transactions as CSV (UTF-8 with BOM so spreadsheet apps read it right). A text cell that
 * starts like a formula is prefixed with ' so it is never executed when opened (CSV injection).
 */
final class TransactionCsvWriter {

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String[] HEADER_EN = {"Date", "Type", "Category", "Amount", "Payment method", "Description", "Reference"};
    private static final String[] HEADER_ID = {"Tanggal", "Jenis", "Kategori", "Jumlah", "Metode pembayaran", "Keterangan", "No. referensi"};

    private TransactionCsvWriter() {
    }

    static byte[] write(List<Transaction> transactions, Map<String, FinanceCategory> categories, AppLanguage language) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(BOM);
        try (CSVWriter csv = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            csv.writeNext(language == AppLanguage.ID ? HEADER_ID : HEADER_EN);
            for (Transaction t : transactions) {
                FinanceCategory category = categories.get(t.getCategoryId());
                csv.writeNext(new String[]{
                        String.valueOf(t.getLocalDate()),
                        t.getTransactionType() != null ? t.getTransactionType().name() : "",
                        safe(category != null ? category.getName() : t.getTransactionCategory()),
                        t.getAmount() != null ? t.getAmount().abs().toPlainString() : "",
                        t.getPaymentMethod() != null ? t.getPaymentMethod().name() : "",
                        safe(t.getTransactionDesc()),
                        safe(t.getReferenceNumber()),
                });
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /** Neutralises cells a spreadsheet would read as a formula. */
    static String safe(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        char first = value.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r'
                ? "'" + value : value;
    }
}
