package com.stockmaster.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "PRODUCTO")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id;

    @Column(name = "nombre_producto", nullable = false, unique = true)
    private String name;

    @Column(name = "descripcion")
    private String description;

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @Column(name = "precio")
    private double price;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    @JsonIgnoreProperties("hibernateLazyInitializer")
    private Category category;


}
