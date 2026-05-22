package com.stockmaster.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryStockChartDto {
    private String categoryName;
    private Long totalProducts;
}