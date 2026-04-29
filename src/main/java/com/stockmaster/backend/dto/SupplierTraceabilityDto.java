package com.stockmaster.backend.dto;

import lombok.Data;

@Data
public class SupplierTraceabilityDto {
    private String productName;
    private String categoryName;
    private int totalStock;
    private String warehouseName;
}
