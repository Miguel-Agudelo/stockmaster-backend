package com.stockmaster.backend.controller;

import com.stockmaster.backend.dto.WarehouseStockDto;
import com.stockmaster.backend.service.InventoryMovementService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@SecurityRequirement(name = "BearerAuth")
public class InventoryController {

    @Autowired
    private InventoryMovementService inventoryMovementService;

    /**
     * Devuelve el stock de un producto desglosado por almacén.
     * Usado por StockMovementForm y StockTransferPage.
     */
    @GetMapping("/stock-by-product/{productId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<WarehouseStockDto>> getProductStockByWarehouses(@PathVariable Long productId) {
        try {
            List<WarehouseStockDto> stockDetails = inventoryMovementService.getProductStockByWarehouses(productId);
            return ResponseEntity.ok(stockDetails);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * HU11 — Devuelve el stock actual de un producto en un almacén concreto.
     * Permite al frontend mostrar "Stock actual: X uds" en tiempo real
     * al seleccionar producto + almacén en el formulario de ajuste.
     */
    @GetMapping("/current-stock")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<?> getCurrentStock(
            @RequestParam Long productId,
            @RequestParam Long warehouseId) {
        try {
            int stock = inventoryMovementService.getCurrentStock(productId, warehouseId);
            return ResponseEntity.ok(Map.of("currentStock", stock));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
