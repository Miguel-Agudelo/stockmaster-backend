package com.stockmaster.backend.repository;

import com.stockmaster.backend.entity.ProductChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductChangeLogRepository extends JpaRepository<ProductChangeLog, Long> {

    // Obtiene el historial de un producto ordenado por fecha descendente (más reciente primero)
    List<ProductChangeLog> findByProductIdOrderByChangedAtDesc(Long productId);
}