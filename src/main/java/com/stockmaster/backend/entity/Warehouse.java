package com.stockmaster.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "ALMACEN")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_almacen")
    private Long id;

    @Column(name = "nombre_almacen", nullable = false, unique = true)
    private String name;

    @Column(name = "direccion")
    private String address;

    @Column(name = "ciudad")
    private String city;

    @Column(name = "descripcion")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
