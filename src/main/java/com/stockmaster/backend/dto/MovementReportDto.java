package com.stockmaster.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MovementReportDto {
    private LocalDateTime movementDate;
    private String productName;
    private String movementType;
    private int quantity;
    private String warehouseName;
    private String userName;
    //private String transferReference;
}
