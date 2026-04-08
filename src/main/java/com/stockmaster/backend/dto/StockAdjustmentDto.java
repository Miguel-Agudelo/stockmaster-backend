package com.stockmaster.backend.dto;

import lombok.Data;

@Data
public class StockAdjustmentDto {

    private Long productId;
    private Long warehouseId;
    private Long userId;

    /** "POSITIVO" o "NEGATIVO" */
    private String adjustmentType;

    private int quantity;

    /** "CONTEO_FISICO", "PRODUCTO_DANADO", "PERDIDA", "ERROR_REGISTRO", "OTRA_RAZON" */
    private String reason;

    /** Descripción libre (opcional) */
    private String notes;
}
