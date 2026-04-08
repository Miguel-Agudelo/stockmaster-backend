package com.stockmaster.backend.dto;

import lombok.Data;

@Data
public class SupplierListDto {
    private Long id;
    private String name;
    private String nit;
    private String phone;
    private String email;
    private String address;
    private boolean active;  // Nota: NO usar "isActive" — Lombok lo serializa como "active" en JSON
    // Cuántos productos activos tiene asociados
    private long productCount;
}
