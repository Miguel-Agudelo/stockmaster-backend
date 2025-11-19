package com.stockmaster.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MovementDto {
    private Long productId;
    private Long warehouseId;
    private int quantity;
    private Long userId;
    private Long id;
    private String movementType;
    private String motive;
    private String transferReference;
    private LocalDateTime movementDate;
    private String productName;
    private String warehouseName;
    private String userName;
}