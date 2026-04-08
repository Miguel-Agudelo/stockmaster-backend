package com.stockmaster.backend.repository;

import com.stockmaster.backend.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    // Solo proveedores activos para el listado principal
    List<Supplier> findByIsActiveTrue();

    // Verificar nombre único
    Optional<Supplier> findByName(String name);

    // Verificar NIT único (ignorando nulos)
    Optional<Supplier> findByNit(String nit);

    // Cuántos productos activos tiene este proveedor asociados
    @Query("SELECT COUNT(p) FROM Product p JOIN p.suppliers s WHERE s.id = :supplierId AND p.isActive = true")
    long countActiveProductsBySupplierId(@Param("supplierId") Long supplierId);
}
