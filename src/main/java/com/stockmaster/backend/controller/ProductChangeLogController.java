package com.stockmaster.backend.controller;

import com.stockmaster.backend.dto.ProductChangeLogDto;
import com.stockmaster.backend.service.ProductChangeLogService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@SecurityRequirement(name = "BearerAuth")
public class ProductChangeLogController {

    @Autowired
    private ProductChangeLogService changeLogService;

    /**
     * HU-PI2-10: Consultar historial de cambios de un producto.
     * Accesible por ADMINISTRADOR y OPERADOR (solo lectura).
     * GET /api/products/{id}/changelog
     */
    @GetMapping("/{id}/changelog")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<?> getProductChangeLog(@PathVariable Long id) {
        try {
            List<ProductChangeLogDto> logs = changeLogService.getChangeLogByProduct(id);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Error al obtener el historial de cambios."));
        }
    }
}