package com.vyra.virtual_your_assets.dto.chart;

import lombok.Data;

import java.util.List;

@Data
public class GetChartResponse {
    private List<ChartItemResponse> chart;

}
