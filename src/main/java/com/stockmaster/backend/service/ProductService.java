package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.ProductDto;
import com.stockmaster.backend.dto.ProductListDto;
import com.stockmaster.backend.dto.SupplierListDto;
import com.stockmaster.backend.entity.Category;
import com.stockmaster.backend.entity.Inventory;
import com.stockmaster.backend.entity.Product;
import com.stockmaster.backend.entity.Supplier;
import com.stockmaster.backend.entity.Warehouse;
import com.stockmaster.backend.repository.CategoryRepository;
import com.stockmaster.backend.repository.InventoryRepository;
import com.stockmaster.backend.repository.ProductRepository;
import com.stockmaster.backend.repository.SupplierRepository;
import com.stockmaster.backend.repository.WarehouseRepository;
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
            dto.setId((Long) result[0]);
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
            dto.setSuppliers(loadSuppliers((Long) result[0]));
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Product updateProduct(Long id, ProductDto productDto) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        existingProduct.setName(productDto.getName());
        existingProduct.setDescription(productDto.getDescription());
        existingProduct.setPrice(productDto.getPrice());
        existingProduct.setCategory(resolveCategory(productDto));

        // HU-PI2-01: actualizar proveedores si se enviaron (null = no tocar, lista vacía = quitar todos)
        if (productDto.getSupplierIds() != null) {
            existingProduct.setSuppliers(resolveSuppliers(productDto.getSupplierIds()));
        }

        return productRepository.save(existingProduct);
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
            dto.setId((Long) result[0]);
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
