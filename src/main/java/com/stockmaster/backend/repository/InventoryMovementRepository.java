package com.stockmaster.backend.repository;

import com.stockmaster.backend.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByMovementDateBetweenOrderByMovementDateAsc(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT p.id, p.name, SUM(m.quantity), SUM(m.quantity * p.price), " +
            "SUM(m.quantity * p.price) / SUM(m.quantity) " +
            "FROM InventoryMovement m JOIN m.product p " +
            "WHERE m.movementType = 'SALIDA' " +
            "GROUP BY p.id, p.name, p.price " +
            "ORDER BY SUM(m.quantity) DESC")
    List<Object[]> findMostSoldProductsWithRevenueAndAveragePrice();

    long countByMovementDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<InventoryMovement> findByOrderByMovementDateDesc(org.springframework.data.domain.Pageable pageable);

    // Dashboard: movimientos agrupados por mes del año en curso
    @Query("SELECT MONTH(m.movementDate), YEAR(m.movementDate), " +
            "SUM(CASE WHEN m.movementType = 'ENTRADA' THEN m.quantity ELSE 0 END), " +
            "SUM(CASE WHEN m.movementType = 'SALIDA'  THEN m.quantity ELSE 0 END) " +
            "FROM InventoryMovement m " +
            "WHERE YEAR(m.movementDate) = YEAR(CURRENT_DATE) " +
            "GROUP BY YEAR(m.movementDate), MONTH(m.movementDate) " +
            "ORDER BY MONTH(m.movementDate) ASC")
    List<Object[]> findMovimientosMensualesAnioActual();

    // Dashboard: total de ingresos acumulados (todas las salidas)
    @Query("SELECT COALESCE(SUM(m.quantity * p.price), 0) " +
            "FROM InventoryMovement m JOIN m.product p " +
            "WHERE m.movementType = 'SALIDA'")
    Double calculateTotalRevenue();
}