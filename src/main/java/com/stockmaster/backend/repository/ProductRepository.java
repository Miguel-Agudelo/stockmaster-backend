package com.stockmaster.backend.repository;

import com.stockmaster.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByName(String name);

    @Query("SELECT p.id, p.name, p.description, p.price, p.sku, p.category.name, SUM(i.currentStock) " +
            "FROM Product p JOIN p.category c LEFT JOIN Inventory i ON p.id = i.product.id " +
            "WHERE p.isActive = true " +
            "GROUP BY p.id, p.name, p.description, p.price, p.sku, c.name")
    List<Object[]> findAllProductsWithTotalStock();

    @Query("SELECT p.id, p.name, p.description, p.price, p.sku, c.name, SUM(i.currentStock) " +
            "FROM Product p " +
            "LEFT JOIN p.category c " +
            "LEFT JOIN Inventory i ON i.product.id = p.id " +
            "WHERE p.isActive = false " + // <-- Filtro de inactivos
            "GROUP BY p.id, c.name")
    List<Object[]> findAllInactiveProductsWithTotalStock();

    /** HU14: Reporte de stock bajo. Encuentra productos activos
     * cuyo stock total consolidado sea menor que su minStock.
     */
    @Query("SELECT p.id, p.name, SUM(i.currentStock), i.minStock " +
            "FROM Product p JOIN Inventory i ON p.id = i.product.id " +
            "WHERE p.isActive = true " +
            "GROUP BY p.id, p.name, i.minStock " +
            "HAVING SUM(i.currentStock) < MIN(i.minStock)")
    List<Object[]> findProductsBelowMinStock();

}
