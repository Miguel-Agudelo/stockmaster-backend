package com.stockmaster.backend.repository;

import com.stockmaster.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    // Todas las categorías raíz (sin padre)
    List<Category> findByParentCategoryIsNull();

    // Subcategorías de una categoría padre
    List<Category> findByParentCategory(Category parent);

    // Cuántos productos activos usa esta categoría
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true")
    long countActiveProductsByCategoryId(@Param("categoryId") Long categoryId);

    // Verificar si una categoría tiene subcategorías
    boolean existsByParentCategoryId(Long parentId);
}
