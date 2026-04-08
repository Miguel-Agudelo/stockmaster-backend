package com.stockmaster.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProductDto {
    private String name;
    private String description;
    private double price;
    private Long categoryId;
    private String categoryName; // Legado
    private int initialQuantity;
    private Long warehouseId;
    private int minStock;
    // HU-PI2-01: IDs de proveedores a asociar al producto
    private List<Long> supplierIds;
}
