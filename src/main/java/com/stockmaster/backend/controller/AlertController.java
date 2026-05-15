package com.stockmaster.backend.controller;

import com.stockmaster.backend.dto.LowStockProductDto;
import com.stockmaster.backend.entity.Inventory;
import com.stockmaster.backend.repository.InventoryRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alerts")
@SecurityRequirement(name = "BearerAuth")
public class AlertController {

    @Autowired
    private InventoryRepository inventoryRepository;

    /**
     * Devuelve todos los registros de inventario cuyo stock actual
     * es menor o igual al stock mínimo configurado.
     * Solo accesible para ADMINISTRADOR.
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<LowStockProductDto>> getLowStockAlerts() {
        List<Inventory> items = inventoryRepository.findItemsWithLowStock();
        List<LowStockProductDto> dtos = items.stream()
                .map(item -> new LowStockProductDto(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getWarehouse().getName(),
                        item.getCurrentStock(),
                        item.getMinStock()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
