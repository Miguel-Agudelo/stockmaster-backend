package com.stockmaster.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonthlyMovementsDto {
    private int mes;
    private int anio;
    private Long entradas;
    private Long salidas;
}