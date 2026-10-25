package com.vyra.be_virtual_your_assets.dto.category;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import lombok.Data;

import java.time.LocalDateTime;

/** A built-in category as the portal manages it (including deactivated ones). */
@Data
public class AdminCategoryResponse {
    private String id;
    /** Seeded categories only; the apps translate their name by it. */
    private String code;
    private String name;
    private TransactionType type;
    private String icon;
    private String color;
    private int position;
    /** false = hidden from members (existing transactions keep showing it). */
    private boolean active;
    private LocalDateTime updatedAt;
}
