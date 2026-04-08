package com.stockmaster.backend.controller;

import com.stockmaster.backend.dto.SupplierDto;
import com.stockmaster.backend.dto.SupplierListDto;
import com.stockmaster.backend.service.SupplierService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
@SecurityRequirement(name = "BearerAuth")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<SupplierListDto>> getAllActiveSuppliers() {
        return ResponseEntity.ok(supplierService.getAllActiveSuppliers());
    }

    // Papelera: listar inactivos
    @GetMapping("/inactive")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<SupplierListDto>> getAllInactiveSuppliers() {
        return ResponseEntity.ok(supplierService.getAllInactiveSuppliers());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> createSupplier(@RequestBody SupplierDto dto) {
        try {
            return new ResponseEntity<>(supplierService.createSupplier(dto), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> updateSupplier(@PathVariable Long id, @RequestBody SupplierDto dto) {
        try {
            return ResponseEntity.ok(supplierService.updateSupplier(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    // Papelera: restaurar proveedor inactivo
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> restoreSupplier(@PathVariable Long id) {
        try {
            supplierService.restoreSupplier(id);
            return ResponseEntity.ok(Map.of("message", "Proveedor restaurado exitosamente."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    // Desactivar (mover a papelera)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deactivateSupplier(@PathVariable Long id) {
        try {
            supplierService.deactivateSupplier(id);
            return ResponseEntity.ok(Map.of("message", "Proveedor desactivado exitosamente."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }
}
