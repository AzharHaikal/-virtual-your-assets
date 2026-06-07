package com.vyra.be_virtual_your_assets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponse<T> {
    private String responseStatus;
    private String responseMessage;
    private T data;

}
