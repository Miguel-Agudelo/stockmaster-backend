package com.stockmaster.backend.repository;

import com.stockmaster.backend.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    // HU15: Reporte de movimientos por rango de fechas
    List<InventoryMovement> findByMovementDateBetweenOrderByMovementDateAsc(LocalDateTime startDate, LocalDateTime endDate);


    /** HU16: Reporte de productos más vendidos. Suma la cantidad total de SALIDAS.
     * Calcula ingresos usando el precio actual del producto.
     */
    @Query("SELECT p.id, p.name, SUM(m.quantity), p.price * SUM(m.quantity) " +
            "FROM InventoryMovement m JOIN m.product p " +
            "WHERE m.movementType = 'SALIDA' " +
            "GROUP BY p.id, p.name, p.price " +
            "ORDER BY SUM(m.quantity) DESC")
    List<Object[]> findMostSoldProductsWithRevenue();
}
