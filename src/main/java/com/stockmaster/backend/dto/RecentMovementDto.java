package com.stockmaster.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class RecentMovementDto {
    private Long id;
    private String productName;
    private String warehouseName;
    private int quantity;
    private LocalDate date;
    private String userName;
}