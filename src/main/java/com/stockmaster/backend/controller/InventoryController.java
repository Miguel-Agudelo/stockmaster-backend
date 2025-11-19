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

@RestController
@RequestMapping("/api/inventory")
@SecurityRequirement(name = "BearerAuth")
public class InventoryController {

    @Autowired
    private InventoryMovementService inventoryMovementService;

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
}