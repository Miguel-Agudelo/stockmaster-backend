package com.stockmaster.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopProductStockDto {
    private String productName;
    private Long totalStock;
}