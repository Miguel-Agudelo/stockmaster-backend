package com.stockmaster.backend.repository;

import com.stockmaster.backend.entity.Inventory;
import com.stockmaster.backend.entity.Product;
import com.stockmaster.backend.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductAndWarehouse(Product product, Warehouse warehouse);
    long countByWarehouse(Warehouse warehouse);
    List<Inventory> findByWarehouseAndCurrentStockGreaterThan(Warehouse warehouse, int currentStock);
    long countByWarehouseAndCurrentStockGreaterThan(Warehouse warehouse, int currentStock);

    @Query("SELECT i FROM Inventory i WHERE i.currentStock < i.minStock")
    List<Inventory> findItemsWithLowStock();

    @Query("SELECT SUM(i.currentStock) FROM Inventory i")
    Long calculateTotalStock();
}
