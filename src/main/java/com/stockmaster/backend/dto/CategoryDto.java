package com.stockmaster.backend.dto;

import lombok.Data;

@Data
public class CategoryDto {
    private String name;
    // null = categoría raíz. Si tiene valor = subcategoría de esa categoría padre.
    private Long parentCategoryId;
}
