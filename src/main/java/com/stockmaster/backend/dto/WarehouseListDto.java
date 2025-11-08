package com.stockmaster.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

import java.util.List;

@Data
public class WarehouseListDto {
    private Long id;
    private String name;
    private String address;
    private String city;
    private String description;
    private int totalStock;
    private List<ProductStockDto> products;
    private LocalDateTime deletedAt;
}
