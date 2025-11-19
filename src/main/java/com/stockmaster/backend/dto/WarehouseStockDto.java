package com.stockmaster.backend.dto;

import lombok.Data;

@Data
public class WarehouseStockDto {
    private Long warehouseId;
    private int currentStock;

    public WarehouseStockDto(Long warehouseId, int currentStock) {
        this.warehouseId = warehouseId;
        this.currentStock = currentStock;
    }
}