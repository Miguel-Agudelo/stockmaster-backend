package com.stockmaster.backend.controller;

import com.stockmaster.backend.dto.MovementDto;
import com.stockmaster.backend.dto.TransferDto;
import com.stockmaster.backend.entity.InventoryMovement;
import com.stockmaster.backend.service.InventoryMovementService;
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
@RequestMapping("/api/movements")
@SecurityRequirement(name = "BearerAuth")
public class InventoryMovementController {

    @Autowired
    private InventoryMovementService movementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<MovementDto>> getMovementHistory() {
        List<MovementDto> history = movementService.getAllMovements();
        return ResponseEntity.ok(history);
    }

    // HU08 - Registro de entradas
    @PostMapping("/entry")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<?> registerEntry(@RequestBody MovementDto movementDto) {
        try {
            InventoryMovement movement = movementService.registerEntry(movementDto);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Entrada de stock registrada exitosamente.");
            response.put("movement", movement);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // HU09 - Registro de salidas
    @PostMapping("/exit")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<?> registerExit(@RequestBody MovementDto movementDto) {
        try {
            InventoryMovement movement = movementService.registerExit(movementDto);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Salida de stock registrada exitosamente.");
            response.put("movement", movement);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // HU20 - Transferencia de stock entre almacenes
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<?> transferStock(@RequestBody TransferDto transferDto) {
        try {
            Map<String, Object> result = movementService.transferStock(transferDto);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Transferencia de stock registrada exitosamente.");
            response.put("transferReference", result.get("transferReference"));
            response.put("exitMovement", result.get("exitMovement"));
            response.put("entryMovement", result.get("entryMovement"));
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}