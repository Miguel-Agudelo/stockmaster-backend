package com.stockmaster.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductListDto {
    private Long id;
    private String name;
    private String description;
    private double price;
    private String categoryName;
    private Long categoryId;
    private int totalStock;
    private String sku;
    private LocalDateTime deletedAt;
    // HU-PI2-01: proveedores asociados al producto
    private List<SupplierListDto> suppliers;
}
