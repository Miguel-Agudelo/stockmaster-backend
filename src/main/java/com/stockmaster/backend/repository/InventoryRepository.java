package com.stockmaster.backend.repository;

import com.stockmaster.backend.dto.TopProductStockDto;
import com.stockmaster.backend.entity.Inventory;
import com.stockmaster.backend.entity.Product;
import com.stockmaster.backend.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.stockmaster.backend.dto.WarehouseStockChartDto;
import com.stockmaster.backend.dto.CategoryStockChartDto;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductAndWarehouse(Product product, Warehouse warehouse);
    long countByWarehouse(Warehouse warehouse);
    List<Inventory> findByWarehouseAndCurrentStockGreaterThan(Warehouse warehouse, int currentStock);
    long countByWarehouseAndCurrentStockGreaterThan(Warehouse warehouse, int currentStock);

    @Query("SELECT i FROM Inventory i WHERE i.currentStock <= i.minStock")
    List<Inventory> findItemsWithLowStock();

    @Query("SELECT SUM(i.currentStock) FROM Inventory i")
    Long calculateTotalStock();

    List<Inventory> findByProductId(Long productId);

    // HU-PI2-09
    @Query("SELECT i FROM Inventory i JOIN i.product p JOIN p.suppliers s WHERE s.id = :supplierId AND p.isActive = true ORDER BY p.name ASC")
    List<Inventory> findBySupplierId(@Param("supplierId") Long supplierId);

    // HU-PI2-03: Stock total agrupado por bodega activa (para gráfico de barras)
    @Query("SELECT new com.stockmaster.backend.dto.WarehouseStockChartDto(w.name, COALESCE(SUM(i.currentStock), 0)) " +
            "FROM Warehouse w LEFT JOIN Inventory i ON i.warehouse.id = w.id " +
            "WHERE w.isActive = true " +
            "GROUP BY w.id, w.name " +
            "ORDER BY w.name ASC")
    List<WarehouseStockChartDto> findStockByActiveWarehouse();

    // HU-PI2-03: Total de productos activos agrupados por categoría (para gráfico circular)
    @Query("SELECT new com.stockmaster.backend.dto.CategoryStockChartDto(c.name, COUNT(p.id)) " +
            "FROM Product p JOIN p.category c " +
            "WHERE p.isActive = true " +
            "GROUP BY c.id, c.name " +
            "ORDER BY COUNT(p.id) DESC")
    List<CategoryStockChartDto> findProductCountByCategory();

    // HU-PI2-03: Valor total del inventario (precio * stock_actual)
    @Query("SELECT SUM(i.currentStock * i.product.price) FROM Inventory i WHERE i.product.isActive = true")
    Double calculateTotalInventoryValue();

    // Dashboard: top 7 productos con mayor stock disponible
    @Query("SELECT new com.stockmaster.backend.dto.TopProductStockDto(" +
            "    i.product.name, SUM(i.currentStock)) " +
            "FROM Inventory i " +
            "WHERE i.product.isActive = true " +
            "GROUP BY i.product.id, i.product.name " +
            "ORDER BY SUM(i.currentStock) DESC")
    List<TopProductStockDto> findTopProductosByStock(org.springframework.data.domain.Pageable pageable);
}
