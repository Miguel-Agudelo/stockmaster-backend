package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.ProductDto;
import com.stockmaster.backend.dto.ProductListDto;
import com.stockmaster.backend.entity.Category;
import com.stockmaster.backend.entity.Inventory;
import com.stockmaster.backend.entity.Product;
import com.stockmaster.backend.entity.Warehouse;
import com.stockmaster.backend.repository.CategoryRepository;
import com.stockmaster.backend.repository.InventoryRepository;
import com.stockmaster.backend.repository.ProductRepository;
import com.stockmaster.backend.repository.WarehouseRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private InventoryRepository inventoryRepository;

    @Transactional
    public Product createProduct(ProductDto productDto) {
        // Criterio de Aceptación: El sistema valida que los campos obligatorios estén completos antes de guardar.
        if (productDto.getName() == null || productDto.getName().isEmpty() ||
                productDto.getCategoryName() == null || productDto.getCategoryName().isEmpty() ||
                productDto.getWarehouseId() == null) {
            throw new IllegalArgumentException("Los campos de nombre, categoría y almacén son obligatorios.");
        }

        // Se verifica si ya existe un producto con el mismo nombre
        if (productRepository.findByName(productDto.getName()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un producto con este nombre.");
        }

        // Criterio de Aceptación: El formulario de creación de producto debe incluir campos para nombre, descripción, precio, categoría y cantidad inicial.
        Product product = new Product();
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());

        // Se verifica si la categoría ya existe, si no, se crea
        Category category = categoryRepository.findByName(productDto.getCategoryName())
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(productDto.getCategoryName());
                    return categoryRepository.save(newCategory);
                });
        product.setCategory(category);

        // Se genera un SKU único para el producto
        String sku = generateSku(productDto.getName());
        product.setSku(sku);

        Product savedProduct = productRepository.save(product);

        // Criterio de Aceptación: La cantidad inicial se asigna a un almacén específico.
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

    // Método para la HU05
    public List<ProductListDto> getAllProducts() {
        List<Object[]> results = productRepository.findAllProductsWithTotalStock();

        return results.stream()
                .map(result -> {
                    ProductListDto dto = new ProductListDto();
                    dto.setId((Long) result[0]);
                    dto.setName((String) result[1]);
                    dto.setDescription((String) result[2]);
                    dto.setPrice((Double) result[3]);
                    dto.setSku((String) result[4]);
                    dto.setCategoryName((String) result[5]);
                    // Se valida si el stock total es nulo (si no hay registros de inventario)
                    Long totalStockLong = (Long) result[6];
                    dto.setTotalStock(totalStockLong != null ? totalStockLong.intValue() : 0);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // Método para la HU06
    public Product updateProduct(Long id, ProductDto productDto) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        // Criterio de Aceptación: Puede modificar nombre, descripción, precio y categoría.
        existingProduct.setName(productDto.getName());
        existingProduct.setDescription(productDto.getDescription());
        existingProduct.setPrice(productDto.getPrice());

        // Actualiza la categoría
        Category newCategory = categoryRepository.findByName(productDto.getCategoryName())
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName(productDto.getCategoryName());
                    return categoryRepository.save(category);
                });
        existingProduct.setCategory(newCategory);

        return productRepository.save(existingProduct);
    }

    // Método para la HU07
    @Transactional
    public void deleteProduct(Long id) {
        Product productToDelete = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        // Se establece el estado a inactivo en lugar de eliminar el registro
        productToDelete.setActive(false);
        productRepository.save(productToDelete);
    }

    private String generateSku(String productName) {
        // Lógica simple para generar un SKU. Se puede ajustar para otra convención.
        return productName.toUpperCase().replace(" ", "-") + "-" + System.currentTimeMillis();
    }

    // HU17. Restaurar Producto
    @Transactional
    public void restoreProduct(Long id) {
        Product productToRestore = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
        if (productToRestore.isActive()) {
            throw new IllegalArgumentException("El producto ya se encuentra activo.");
        }
        productToRestore.setActive(true);
        productRepository.save(productToRestore);

    }

    // HU17 . Listar productos inactivos
    public List<ProductListDto> getAllInactiveProducts() {
        List<Object[]> results = productRepository.findAllInactiveProductsWithTotalStock();

        return results.stream()
                .map(result -> {
                    ProductListDto dto = new ProductListDto();
                    dto.setId((Long) result[0]);
                    dto.setName((String) result[1]);
                    dto.setDescription((String) result[2]);
                    dto.setPrice((Double) result[3]);
                    dto.setSku((String) result[4]);
                    dto.setCategoryName((String) result[5]);
                    Long totalStockLong = (Long) result[6];
                    dto.setTotalStock(totalStockLong != null ? totalStockLong.intValue() : 0);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
