package com.stockmaster.backend.controller;

import com.stockmaster.backend.dto.WarehouseDto;
import com.stockmaster.backend.dto.WarehouseListDto;
import com.stockmaster.backend.entity.Warehouse;
import com.stockmaster.backend.service.WarehouseService;
import com.stockmaster.backend.dto.WarehouseSelectionDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/warehouses")
@SecurityRequirement(name = "BearerAuth")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    @GetMapping("/active-list") // 💡 ESTA ES LA RUTA QUE FALTABA
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')") // 💡 PERMISOS PARA EVITAR EL 403
    public ResponseEntity<List<WarehouseSelectionDto>> getActiveWarehousesList() {
        List<WarehouseSelectionDto> warehouses = warehouseService.getActiveWarehousesForSelection();
        return ResponseEntity.ok(warehouses);
    }
    // HU12 - Registro de almacenes (Solo Administrador)
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> createWarehouse(@RequestBody WarehouseDto warehouseDto) {
        try {
            Warehouse newWarehouse = warehouseService.createWarehouse(warehouseDto);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Almacén registrado exitosamente.");
            response.put("warehouse", newWarehouse);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // HU11 - Actualización de almacenes
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<?> updateWarehouse(@PathVariable Long id, @RequestBody WarehouseDto warehouseDto) {
        try {
            Warehouse updatedWarehouse = warehouseService.updateWarehouse(id, warehouseDto);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Almacén actualizado exitosamente.");
            response.put("warehouse", updatedWarehouse);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    // HU13 - Eliminación de almacenes
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteWarehouse(@PathVariable Long id) {
        try {
            warehouseService.deleteWarehouse(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Almacén eliminado exitosamente.");
            return ResponseEntity.ok().body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // HU10 - Visualización de almacenes
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<WarehouseListDto>> getAllWarehouses() {
        List<WarehouseListDto> warehouses = warehouseService.getAllWarehouses();
        return ResponseEntity.ok(warehouses);
    }
}
