package com.stockmaster.backend.dto;

import lombok.Data;

@Data
public class StockReportDto {
    private Long productId;
    private String productName;
    private String warehouseName;
    private int currentStock;
    private int minimumStock;
}
