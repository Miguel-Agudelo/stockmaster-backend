package com.stockmaster.backend.repository;

import com.stockmaster.backend.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByName(String name);
    List<Warehouse> findByIsActiveTrue();

    @Query("SELECT w FROM Warehouse w WHERE w.isActive = true")
    List<Warehouse> findActiveWarehousesForSelection();
    // HU10: Consulta para obtener almacenes activos con su stock total
    @Query("SELECT w.id, w.name, w.address, w.city, w.description, SUM(i.currentStock) " +
            "FROM Warehouse w LEFT JOIN Inventory i ON w.id = i.warehouse.id " +
            "WHERE w.isActive = true " +
            "GROUP BY w.id, w.name, w.address, w.city, w.description")
    List<Object[]> findAllActiveWarehousesWithTotalStock();

    // HU18: Consulta para obtener almacenes inactivos con su stock total
    @Query("SELECT w.id, w.name, w.address, w.city, w.description, SUM(i.currentStock) " +
            "FROM Warehouse w LEFT JOIN Inventory i ON w.id = i.warehouse.id " +
            "WHERE w.isActive = false " + // <-- Filtro de inactivos
            "GROUP BY w.id, w.name, w.address, w.city, w.description")
    List<Object[]> findAllInactiveWarehousesWithTotalStock();
}