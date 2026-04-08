package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.CategoryDto;
import com.stockmaster.backend.dto.CategoryListDto;
import com.stockmaster.backend.entity.Category;
import com.stockmaster.backend.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    // Devuelve todas las categorías (raíces y subcategorías) con su info de jerarquía
    public List<CategoryListDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toListDto)
                .collect(Collectors.toList());
    }

    // Solo las categorías raíz (para el selector de "categoría padre")
    public List<CategoryListDto> getRootCategories() {
        return categoryRepository.findByParentCategoryIsNull().stream()
                .map(this::toListDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryListDto createCategory(CategoryDto dto) {
        // Validar nombre obligatorio
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio.");
        }
        // Validar nombre único
        if (categoryRepository.findByName(dto.getName().trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + dto.getName());
        }

        Category category = new Category();
        category.setName(dto.getName().trim());

        // Si se envía un padre, asignarlo (subcategoría)
        if (dto.getParentCategoryId() != null) {
            Category parent = categoryRepository.findById(dto.getParentCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("La categoría padre con ID "
                            + dto.getParentCategoryId() + " no existe."));
            // Validar que el padre no sea ya una subcategoría (solo 2 niveles)
            if (parent.getParentCategory() != null) {
                throw new IllegalArgumentException("No se pueden crear subcategorías de subcategorías. Solo se permiten dos niveles.");
            }
            category.setParentCategory(parent);
        }

        return toListDto(categoryRepository.save(category));
    }

    @Transactional
    public CategoryListDto updateCategory(Long id, CategoryDto dto) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con ID: " + id));

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio.");
        }

        // Validar nombre único (ignorando la propia categoría)
        categoryRepository.findByName(dto.getName().trim()).ifPresent(found -> {
            if (!found.getId().equals(id)) {
                throw new IllegalArgumentException("Ya existe otra categoría con el nombre: " + dto.getName());
            }
        });

        existing.setName(dto.getName().trim());

        // Actualizar padre si se envía
        if (dto.getParentCategoryId() != null) {
            if (dto.getParentCategoryId().equals(id)) {
                throw new IllegalArgumentException("Una categoría no puede ser su propio padre.");
            }
            Category parent = categoryRepository.findById(dto.getParentCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("La categoría padre no existe."));
            if (parent.getParentCategory() != null) {
                throw new IllegalArgumentException("No se pueden crear subcategorías de subcategorías.");
            }
            existing.setParentCategory(parent);
        } else {
            // Si se envía null explícitamente, se convierte en categoría raíz
            existing.setParentCategory(null);
        }

        return toListDto(categoryRepository.save(existing));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con ID: " + id));

        // Criterio HU-PI2-02: no eliminar si tiene productos activos asociados
        long productCount = categoryRepository.countActiveProductsByCategoryId(id);
        if (productCount > 0) {
            throw new IllegalStateException("No se puede eliminar la categoría '"
                    + category.getName() + "' porque tiene " + productCount
                    + " producto(s) activo(s) asociado(s). Reasigna los productos antes de eliminarla.");
        }

        // No eliminar si tiene subcategorías
        if (categoryRepository.existsByParentCategoryId(id)) {
            throw new IllegalStateException("No se puede eliminar la categoría '"
                    + category.getName() + "' porque tiene subcategorías asociadas. Elimina primero las subcategorías.");
        }

        categoryRepository.deleteById(id);
    }

    // Conversión de entidad a DTO de lista
    private CategoryListDto toListDto(Category category) {
        CategoryListDto dto = new CategoryListDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        if (category.getParentCategory() != null) {
            dto.setParentCategoryId(category.getParentCategory().getId());
            dto.setParentCategoryName(category.getParentCategory().getName());
        }
        dto.setProductCount(categoryRepository.countActiveProductsByCategoryId(category.getId()));
        return dto;
    }
}
