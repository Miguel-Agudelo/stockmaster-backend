package com.stockmaster.backend.dto;

import lombok.Data;

@Data
public class ProductDto {
    private String name;
    private String description;
    private double price;
    private String categoryName;
    private int initialQuantity;
    private Long warehouseId;
    private int minStock;
}
