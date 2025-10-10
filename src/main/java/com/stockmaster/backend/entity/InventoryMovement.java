package com.stockmaster.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "MOVIMIENTO_INVENTARIO")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long id;

    @Column(name = "fecha_movimiento", nullable = false)
    private LocalDateTime movementDate = LocalDateTime.now();

    // Tipo de movimiento: "ENTRADA" o "SALIDA"
    @Column(name = "tipo_movimiento", nullable = false)
    private String movementType;

    @Column(name = "cantidad", nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_almacen", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;
}