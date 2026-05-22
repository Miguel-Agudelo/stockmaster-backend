package com.stockmaster.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WarehouseStockChartDto {
    private String warehouseName;
    private Long totalStock;
}