// Java
package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.ProductDto;
import com.stockmaster.backend.dto.ProductListDto;
import com.stockmaster.backend.dto.SupplierListDto;
import com.stockmaster.backend.entity.Category;
import com.stockmaster.backend.entity.Inventory;
import com.stockmaster.backend.entity.Product;
import com.stockmaster.backend.entity.Supplier;
import com.stockmaster.backend.entity.Warehouse;
import com.stockmaster.backend.entity.User; // <-- IMPORT AGREGADO
import com.stockmaster.backend.repository.CategoryRepository;
import com.stockmaster.backend.repository.InventoryRepository;
import com.stockmaster.backend.repository.ProductRepository;
import com.stockmaster.backend.repository.SupplierRepository;
import com.stockmaster.backend.repository.WarehouseRepository;
import com.stockmaster.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private ProductChangeLogService changeLogService;
    @Autowired private UserRepository userRepository;

    @Transactional
    public Product createProduct(ProductDto productDto) {
        if (productDto.getName() == null || productDto.getName().isEmpty() ||
                productDto.getWarehouseId() == null) {
            throw new IllegalArgumentException("Los campos de nombre y almacén son obligatorios.");
        }
        if (productRepository.findByName(productDto.getName()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un producto con este nombre.");
        }

        Product product = new Product();
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setCategory(resolveCategory(productDto));
        product.setSku(generateSku(productDto.getName()));

        // HU-PI2-01: asociar proveedores si se enviaron
        if (productDto.getSupplierIds() != null && !productDto.getSupplierIds().isEmpty()) {
            product.setSuppliers(resolveSuppliers(productDto.getSupplierIds()));
        }

        Product savedProduct = productRepository.save(product);

        Warehouse warehouse = warehouseRepository.findById(productDto.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado."));

        Inventory initialInventory = new Inventory();
        initialInventory.setProduct(savedProduct);
        initialInventory.setWarehouse(warehouse);
        initialInventory.setCurrentStock(productDto.getInitialQuantity());
        initialInventory.setMinStock(productDto.getMinStock());
        inventoryRepository.save(initialInventory);

        return savedProduct;
    }

    public List<ProductListDto> getAllProducts() {
        List<Object[]> results = productRepository.findAllProductsWithTotalStock();
        return results.stream().map(result -> {
            ProductListDto dto = new ProductListDto();
            Long productId = ((Number) result[0]).longValue();
            dto.setId(productId);
            dto.setName((String) result[1]);
            dto.setDescription((String) result[2]);
            dto.setPrice((Double) result[3]);
            dto.setSku((String) result[4]);
            dto.setCategoryName((String) result[5]);
            Long totalStockLong = (Long) result[6];
            dto.setTotalStock(totalStockLong != null ? totalStockLong.intValue() : 0);
            if (result.length > 7 && result[7] != null) {
                dto.setCategoryId((Long) result[7]);
            }
            // HU-PI2-01: cargar proveedores del producto
            dto.setSuppliers(loadSuppliers(productId));
            if (result.length > 7 && result[7] != null) {
                dto.setCategoryId(((Number) result[7]).longValue());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Product updateProduct(Long id, ProductDto productDto, String editorEmail) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        // Obtener el usuario que realiza el cambio
        User editor = userRepository.findByEmailAndIsActive(editorEmail, true)
                .orElseThrow(() -> new IllegalArgumentException("Usuario editor no encontrado."));

        // ── Detectar y registrar cambios campo por campo ──────────────────
        if (!strEquals(existingProduct.getName(), productDto.getName())) {
            changeLogService.recordChange(existingProduct, editor,
                    "Nombre", existingProduct.getName(), productDto.getName());
            existingProduct.setName(productDto.getName());
        }

        if (!strEquals(existingProduct.getDescription(), productDto.getDescription())) {
            changeLogService.recordChange(existingProduct, editor,
                    "Descripción", existingProduct.getDescription(), productDto.getDescription());
            existingProduct.setDescription(productDto.getDescription());
        }

        String oldPrice = String.valueOf(existingProduct.getPrice());
        String newPrice = String.valueOf(productDto.getPrice());
        if (!strEquals(oldPrice, newPrice)) {
            changeLogService.recordChange(existingProduct, editor,
                    "Precio", oldPrice, newPrice);
            existingProduct.setPrice(productDto.getPrice());
        }

        // Categoría
        Category newCategory = resolveCategory(productDto);
        String oldCatName = existingProduct.getCategory() != null ? existingProduct.getCategory().getName() : null;
        String newCatName = newCategory.getName();
        if (!strEquals(oldCatName, newCatName)) {
            changeLogService.recordChange(existingProduct, editor,
                    "Categoría", oldCatName, newCatName);
            existingProduct.setCategory(newCategory);
        }

        // Proveedores (registrar como un cambio global si hay diferencia)
        if (productDto.getSupplierIds() != null) {
            String oldSuppliers = existingProduct.getSuppliers().stream()
                    .map(s -> s.getName()).sorted().collect(Collectors.joining(", "));
            Set<Supplier> newSupplierSet = resolveSuppliers(productDto.getSupplierIds());
            String newSuppliers = newSupplierSet.stream()
                    .map(s -> s.getName()).sorted().collect(Collectors.joining(", "));
            if (!strEquals(oldSuppliers, newSuppliers)) {
                changeLogService.recordChange(existingProduct, editor,
                        "Proveedores", oldSuppliers.isEmpty() ? "Sin proveedor" : oldSuppliers,
                        newSuppliers.isEmpty() ? "Sin proveedor" : newSuppliers);
                existingProduct.setSuppliers(newSupplierSet);
            }
        }

        return productRepository.save(existingProduct);
    }

    /** Comparación null-safe de strings */
    private boolean strEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product productToDelete = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
        productToDelete.setDeletedAt(LocalDateTime.now());
        productToDelete.setActive(false);
        productRepository.save(productToDelete);
    }

    @Transactional
    public void restoreProduct(Long id) {
        Product productToRestore = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
        if (productToRestore.isActive()) {
            throw new IllegalArgumentException("El producto ya se encuentra activo.");
        }
        productToRestore.setDeletedAt(null);
        productToRestore.setActive(true);
        productRepository.save(productToRestore);
    }

    public List<ProductListDto> getAllInactiveProducts() {
        List<Object[]> results = productRepository.findAllInactiveProductsWithTotalStock();
        return results.stream().map(result -> {
            ProductListDto dto = new ProductListDto();
            Long productId = ((Number) result[0]).longValue();
            dto.setId(productId);
            dto.setName((String) result[1]);
            dto.setDescription((String) result[2]);
            dto.setPrice((Double) result[3]);
            dto.setSku((String) result[4]);
            dto.setCategoryName((String) result[5]);
            Long totalStockLong = (Long) result[6];
            dto.setTotalStock(totalStockLong != null ? totalStockLong.intValue() : 0);
            if (result.length > 7) {
                dto.setDeletedAt((LocalDateTime) result[7]);
            }
            return dto;
        }).collect(Collectors.toList());
    }

    // ── Métodos privados de soporte ──────────────────────────────────────────

    private Category resolveCategory(ProductDto dto) {
        if (dto.getCategoryId() != null) {
            return categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La categoría con ID " + dto.getCategoryId() + " no existe."));
        }
        if (dto.getCategoryName() == null || dto.getCategoryName().isBlank()) {
            throw new IllegalArgumentException("Debe especificar una categoría.");
        }
        return categoryRepository.findByName(dto.getCategoryName())
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(dto.getCategoryName());
                    return categoryRepository.save(newCategory);
                });
    }

    // HU-PI2-01: resolver lista de IDs a entidades Supplier activas
    private Set<Supplier> resolveSuppliers(List<Long> supplierIds) {
        if (supplierIds == null || supplierIds.isEmpty()) return new HashSet<>();
        Set<Supplier> suppliers = new HashSet<>();
        for (Long supplierId : supplierIds) {
            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Proveedor no encontrado con ID: " + supplierId));
            if (!supplier.isActive()) {
                throw new IllegalArgumentException(
                        "El proveedor '" + supplier.getName() + "' está inactivo y no puede asociarse.");
            }
            suppliers.add(supplier);
        }
        return suppliers;
    }

    // HU-PI2-01: cargar proveedores de un producto para el DTO de listado
    private List<SupplierListDto> loadSuppliers(Long productId) {
        return productRepository.findById(productId)
                .map(p -> p.getSuppliers().stream().map(s -> {
                    SupplierListDto dto = new SupplierListDto();
                    dto.setId(s.getId());
                    dto.setName(s.getName());
                    dto.setNit(s.getNit());
                    dto.setPhone(s.getPhone());
                    dto.setEmail(s.getEmail());
                    dto.setAddress(s.getAddress());
                    dto.setActive(s.isActive());
                    return dto;
                }).collect(Collectors.toList()))
                .orElse(List.of());
    }

    private String generateSku(String productName) {
        return productName.toUpperCase().replace(" ", "-") + "-" + System.currentTimeMillis();
    }
}
