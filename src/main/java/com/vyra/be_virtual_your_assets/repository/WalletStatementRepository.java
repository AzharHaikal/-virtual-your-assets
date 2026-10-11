package com.vyra.be_virtual_your_assets.repository;

import com.vyra.be_virtual_your_assets.entity.WalletStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WalletStatementRepository extends JpaRepository<WalletStatement, String> {
    Optional<WalletStatement> findByPhoneNumber(String phoneNumber);

    List<WalletStatement> findByPhoneNumberIn(java.util.Collection<String> phoneNumbers);

    /**
     * NOTE: {@code balance} is stored encrypted via {@code BalanceEncryptConverter},
     * so it CANNOT be aggregated (SUM/AVG) at the SQL/JPQL level - the database only
     * sees ciphertext. totalIncome and totalExpense remain plaintext numeric columns,
     * so those can still be aggregated in SQL, but balance must be summed in Java
     * after Hibernate decrypts each row via the AttributeConverter (see findAllActiveBalances()).
     */
    @Query("SELECT COALESCE(SUM(w.totalIncome), 0) FROM WalletStatement w WHERE w.deletedAt IS NULL")
    BigDecimal sumTotalIncome();

    @Query("SELECT COALESCE(SUM(w.totalExpense), 0) FROM WalletStatement w WHERE w.deletedAt IS NULL")
    BigDecimal sumTotalExpense();

    /** Only the (decrypted) balance column - avoids hydrating whole rows for the dashboard total. */
    @Query("SELECT w.balance FROM WalletStatement w WHERE w.deletedAt IS NULL AND w.balance IS NOT NULL")
    List<BigDecimal> findAllActiveBalances();
}
