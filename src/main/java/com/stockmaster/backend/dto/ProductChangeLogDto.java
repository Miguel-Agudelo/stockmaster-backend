package com.stockmaster.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductChangeLogDto {
    private Long id;
    private Long productId;
    private String productName;
    private String changedByName;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private LocalDateTime changedAt;
}