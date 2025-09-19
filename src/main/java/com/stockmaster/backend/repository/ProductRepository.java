package com.stockmaster.backend.repository;

import com.stockmaster.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByName(String name);

    // Consulta JPQL modificada para incluir el borrado lógico
    @Query("SELECT p.id, p.name, p.description, p.price, p.sku, p.category.name, SUM(i.currentStock) " +
            "FROM Product p JOIN p.category c LEFT JOIN Inventory i ON p.id = i.product.id " +
            "WHERE p.isActive = true " +
            "GROUP BY p.id, p.name, p.description, p.price, p.sku, c.name")
    List<Object[]> findAllProductsWithTotalStock();
}
