package com.stockmaster.backend.dto;

import lombok.Data;

@Data
public class TransferDto {
    private Long productId;
    private Long originWarehouseId;
    private Long destinationWarehouseId;
    private int quantity;
    private Long userId;
    private String motive;
}