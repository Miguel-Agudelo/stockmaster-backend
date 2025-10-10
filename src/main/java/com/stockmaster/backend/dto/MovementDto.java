package com.stockmaster.backend.dto;

import lombok.Data;

@Data
public class MovementDto {
    private Long productId;
    private Long warehouseId;
    private int quantity;
    private Long userId;
}
