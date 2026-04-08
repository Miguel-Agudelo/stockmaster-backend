package com.stockmaster.backend.dto;

import lombok.Data;

@Data
public class CategoryListDto {
    private Long id;
    private String name;
    // null si es categoría raíz
    private Long parentCategoryId;
    private String parentCategoryName;
    // Cuántos productos activos usan esta categoría
    private long productCount;
}
