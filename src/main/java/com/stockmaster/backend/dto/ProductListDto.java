package com.stockmaster.backend.dto;

import lombok.Data;

@Data
public class ProductListDto {
    private Long id;
    private String name;
    private String description;
    private double price;
    private String categoryName;
    private int totalStock;
    private String sku;
}
