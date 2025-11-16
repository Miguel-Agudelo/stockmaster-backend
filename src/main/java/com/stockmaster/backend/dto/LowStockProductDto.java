package com.stockmaster.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class LowStockProductDto {
    private Long id;
    private String name;
    private String warehouseName;
    private int currentStock;
    private int minStock;
}