package com.vyra.be_virtual_your_assets.dto.category;

public final class CategoryRules {
    /** Icon key, e.g. "restaurant" or "directions_car"; the apps map it to an icon. */
    public static final String ICON_PATTERN = "^[a-z][a-z0-9_]{0,39}$";
    public static final String COLOR_PATTERN = "^#[0-9A-Fa-f]{6}$";
    /** Custom categories per member. */
    public static final int MAX_CUSTOM = 50;

    private CategoryRules() {
    }
}
