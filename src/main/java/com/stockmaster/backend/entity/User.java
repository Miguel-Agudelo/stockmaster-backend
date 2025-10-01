package com.stockmaster.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "USUARIO")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "rol", nullable = false)
    private String role;

    @Column(name = "fecha_registro", nullable = false, updatable = false) // 'updatable = false' previene cambios posteriores
    private LocalDateTime createdAt;

    // 💡 ANOTACIÓN: Para actualizar la fecha automáticamente
    @PrePersist
    protected void onCreate() {
        // Establece la fecha y hora actual antes de que la entidad se guarde (persista) por primera vez.
        this.createdAt = LocalDateTime.now();
    }
}
