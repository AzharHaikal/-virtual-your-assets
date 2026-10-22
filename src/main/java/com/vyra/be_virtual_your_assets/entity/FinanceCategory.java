package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A transaction category: built-in ({@link #memberId} null, {@link #code} set, id "default-&lt;CODE&gt;")
 * or a member's custom one. Soft-deleted custom categories stay readable for the transactions using them.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "transaction_finance", name = "finance_category")
public class FinanceCategory extends BaseEntity {

    public static final String DEFAULT_ID_PREFIX = "default-";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Null for a built-in category. */
    private String memberId;

    /** Built-in only: the {@link TransactionCategory} name. */
    @Column(length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    /** Icon key the apps map to an icon (e.g. "restaurant"). */
    @Column(nullable = false, length = 40)
    private String icon;

    /** #RRGGBB */
    @Column(nullable = false, length = 7)
    private String color;

    @Column(nullable = false)
    private int position;

    public boolean isBuiltIn() {
        return memberId == null;
    }

    /**
     * Value for the legacy transaction.transaction_category column: the built-in code, or "Other" of the
     * type for a custom category (older apps and admin reports group by it).
     */
    public String legacyCode() {
        if (code != null) {
            return code;
        }
        return (type == TransactionType.INCOME ? TransactionCategory.OTHERS_INCOME : TransactionCategory.OTHERS_EXPENSE).name();
    }

    public static String defaultId(TransactionCategory category) {
        return DEFAULT_ID_PREFIX + category.name();
    }

    public static String otherId(TransactionType type) {
        return defaultId(type == TransactionType.INCOME ? TransactionCategory.OTHERS_INCOME : TransactionCategory.OTHERS_EXPENSE);
    }
}
