package com.stockmaster.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    // HU-PI2-01: relación N a M con Proveedor a través de PRODUCTO_PROVEEDOR
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "PRODUCTO_PROVEEDOR",
            joinColumns = @JoinColumn(name = "id_producto"),
            inverseJoinColumns = @JoinColumn(name = "id_proveedor")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Set<Supplier> suppliers = new HashSet<>();

    private LocalDateTime createdAt;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime deletedAt;
}
