package com.stockmaster.backend.dto;

import lombok.Data;

@Data
public class SalesReportDto {
    private Long productId;
    private String productName;
    private Long unitsSold; // Cantidad total vendida (SALIDAS)
    private Double totalRevenue; // Ingresos generados (Precio * Cantidad)
    private Double averagePrice;
}
