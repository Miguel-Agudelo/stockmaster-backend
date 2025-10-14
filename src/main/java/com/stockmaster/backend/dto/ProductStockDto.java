package com.stockmaster.backend.dto;

import lombok.Data;

// DTO para representar un producto y su cantidad (stock) dentro de un almacén específico.
@Data
public class ProductStockDto {
    private Long productId;
    private String productName;
    private int currentStock;
    private int minStock;
}