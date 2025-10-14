package com.stockmaster.backend.dto;

import lombok.Data;
import java.time.LocalDateTime; // Necesario para la fecha/hora del movimiento

@Data
public class MovementDto {
    // Campos requeridos para registrar el movimiento (entrada/salida)
    private Long productId;
    private Long warehouseId;
    private int quantity;
    private Long userId;

    // Campos de trazabilidad y visualización (para el historial GET)
    private Long id; // ID del movimiento (opcional, pero útil)
    private String movementType; // "ENTRADA" o "SALIDA"
    private String motive; // El motivo o razón del movimiento
    private LocalDateTime movementDate; // Fecha y hora del movimiento

    // Campos de detalle (para mostrar el nombre en lugar del ID)
    private String productName;
    private String warehouseName;
    private String userName; // Nombre del usuario que registró
}