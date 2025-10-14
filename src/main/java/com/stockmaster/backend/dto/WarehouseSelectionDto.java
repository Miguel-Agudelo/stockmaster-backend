// src/main/java/com/stockmaster/backend/dto/WarehouseSelectionDto.java (NUEVO ARCHIVO)

package com.stockmaster.backend.dto;

public class WarehouseSelectionDto {
    private Long id;
    private String name;

    // Constructor, Getters y Setters
    public WarehouseSelectionDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
