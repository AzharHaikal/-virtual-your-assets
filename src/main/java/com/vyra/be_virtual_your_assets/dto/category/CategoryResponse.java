package com.vyra.be_virtual_your_assets.dto.category;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import lombok.Data;

@Data
public class CategoryResponse {
    private String id;
    /** Built-in only (TransactionCategory name); apps use it to show a translated label. */
    private String code;
    private String name;
    private TransactionType type;
    private String icon;
    private String color;
    /** True for the member's own category (can be renamed / deleted). */
    private boolean custom;
}
