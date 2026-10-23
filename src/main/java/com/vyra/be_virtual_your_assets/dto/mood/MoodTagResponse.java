package com.vyra.be_virtual_your_assets.dto.mood;

import lombok.Data;

@Data
public class MoodTagResponse {
    private String id;
    /** Built-in only; apps use it for the translated label. */
    private String code;
    private String name;
    private boolean custom;
}
