package com.stockmaster.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "HISTORIAL_CAMBIOS_PRODUCTO")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProductChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User changedBy;

    @Column(name = "campo_modificado", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "valor_anterior", length = 1000)
    private String oldValue;

    @Column(name = "valor_nuevo", length = 1000)
    private String newValue;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }
}