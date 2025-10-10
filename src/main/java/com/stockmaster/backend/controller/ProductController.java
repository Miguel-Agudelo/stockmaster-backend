package com.stockmaster.backend.controller;

import com.stockmaster.backend.dto.ProductDto;
import com.stockmaster.backend.dto.ProductListDto;
import com.stockmaster.backend.entity.Product;
import com.stockmaster.backend.service.ProductService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@SecurityRequirement(name = "BearerAuth")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<?> createProduct(@RequestBody ProductDto productDto) {
        try {
            Product newProduct = productService.createProduct(productDto);
            return new ResponseEntity<>(newProduct, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //endpoint para la HU05
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<ProductListDto>> getAllProducts() {
        List<ProductListDto> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    //endpoint para la HU06
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody ProductDto productDto) {
        System.out.println("Antes de @PreAuthorize - ID: " + id + ", Autoridades: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        try {
            Product updatedProduct = productService.updateProduct(id, productDto);
            ProductListDto responseDto = new ProductListDto();
            responseDto.setId(updatedProduct.getId());
            responseDto.setName(updatedProduct.getName());
            responseDto.setDescription(updatedProduct.getDescription());
            responseDto.setPrice(updatedProduct.getPrice());
            responseDto.setCategoryName(updatedProduct.getCategory() != null ? updatedProduct.getCategory().getName() : null);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Endpoint para la HU07 - Borrado Lógico
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Producto eliminado exitosamente.");
            return ResponseEntity.ok().body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
