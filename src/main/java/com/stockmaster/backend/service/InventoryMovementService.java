package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.MovementDto;
import com.stockmaster.backend.dto.TransferDto;
import com.stockmaster.backend.entity.*;
import com.stockmaster.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class InventoryMovementService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InventoryMovementRepository movementRepository;

    @Transactional
    public InventoryMovement registerEntry(MovementDto dto) {
        return registerMovement(dto, "ENTRADA");
    }

    @Transactional
    public InventoryMovement registerExit(MovementDto dto) {
        return registerMovement(dto, "SALIDA");
    }

    private Inventory getAndValidateInventory(Product product, Warehouse warehouse, int quantity, String type) {
        // 1. Obtener o crear el registro de Inventario (stock)
        Inventory inventory = inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .orElseGet(() -> {
                    if (type.equals("SALIDA")) {
                        throw new IllegalStateException("No existe inventario de este producto en el almacén seleccionado.");
                    }
                    Inventory newInventory = new Inventory();
                    newInventory.setProduct(product);
                    newInventory.setWarehouse(warehouse);
                    newInventory.setCurrentStock(0);
                    newInventory.setMinStock(10); // Asumiendo un valor por defecto
                    return newInventory;
                });

        // 2. Aplicar lógica de validación para SALIDA
        if (type.equals("SALIDA") && inventory.getCurrentStock() < quantity) {
            throw new IllegalStateException("Stock insuficiente en el almacén de origen. Hay " + inventory.getCurrentStock() + " unidades y se intenta sacar " + quantity + ".");
        }
        return inventory;
    }

    // Método auxiliar unificado para aplicar la lógica de movimiento y actualizar stock (Base para HU8, HU9 y HU20)
    private InventoryMovement executeMovement(Product product, Warehouse warehouse, User user, int quantity, String type, String transferRef, Inventory inventory) {
        // 1. Actualizar stock
        int newStock = type.equals("ENTRADA")
                ? inventory.getCurrentStock() + quantity
                : inventory.getCurrentStock() - quantity;

        inventory.setCurrentStock(newStock);
        inventoryRepository.save(inventory);

        // 2. Registrar movimiento (trazabilidad)
        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setQuantity(quantity);
        movement.setMovementType(type);
        movement.setUser(user);
        movement.setTransferReference(transferRef); // null si no es transferencia

        return movementRepository.save(movement);
    }

    // Lógica unificada para registrar movimientos simples (HU8, HU9)
    private InventoryMovement registerMovement(MovementDto dto, String type) {
        // 1. Validar y obtener entidades
        Product product = productRepository.findById(dto.getProductId())
                .filter(Product::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado o inactivo."));
        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado."));
        User user = userRepository.findById(dto.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado o inactivo."));
        if (dto.getQuantity() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }

        // 2. Obtener inventario y validar stock (si es salida)
        Inventory inventory = getAndValidateInventory(product, warehouse, dto.getQuantity(), type);

        // 3. Ejecutar el movimiento usando el método unificado (sin referencia de transferencia)
        // El campo transferRef será null, que es correcto para movimientos simples.
        return executeMovement(product, warehouse, user, dto.getQuantity(), type, null, inventory);
    }

    // HU20 - Transferencia de Stock (Transaccional)
    @Transactional
    public Map<String, Object> transferStock(TransferDto dto) {
        if (dto.getOriginWarehouseId().equals(dto.getDestinationWarehouseId())) {
            throw new IllegalArgumentException("El almacén de origen y destino no pueden ser el mismo.");
        }
        if (dto.getQuantity() <= 0) {
            throw new IllegalArgumentException("La cantidad a transferir debe ser mayor a cero.");
        }

        // 1. Validar entidades
        Product product = productRepository.findById(dto.getProductId())
                .filter(Product::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado o inactivo."));
        Warehouse origin = warehouseRepository.findById(dto.getOriginWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Almacén de origen no encontrado."));
        Warehouse destination = warehouseRepository.findById(dto.getDestinationWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Almacén de destino no encontrado."));
        User user = userRepository.findById(dto.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado o inactivo."));

        // 2. Generar Referencia Única de Transferencia (para vincular los dos movimientos)
        String transferReference = "TRANS-" + System.currentTimeMillis();

        // 3. OBTENER INVENTARIO Y VERIFICAR STOCK (Origen y Destino)
        // La validación de stock suficiente se hace en getAndValidateInventory
        Inventory originInventory = getAndValidateInventory(product, origin, dto.getQuantity(), "SALIDA");
        // Para el destino, la cantidad es 0 en la validación, si no existe, se crea el registro con 0
        Inventory destinationInventory = getAndValidateInventory(product, destination, 0, "ENTRADA");

        // 4. EJECUTAR SALIDA (DEL ORIGEN) - El primer paso que puede fallar por stock insuficiente (ya validado)
        InventoryMovement exitMovement = executeMovement(product, origin, user, dto.getQuantity(), "SALIDA", transferReference, originInventory);

        // 5. EJECUTAR ENTRADA (AL DESTINO) - Si la salida fue exitosa, la entrada se ejecuta.
        InventoryMovement entryMovement = executeMovement(product, destination, user, dto.getQuantity(), "ENTRADA", transferReference, destinationInventory);

        // 6. Retornar resultados
        Map<String, Object> result = new HashMap<>();
        result.put("transferReference", transferReference);
        result.put("exitMovement", exitMovement);
        result.put("entryMovement", entryMovement);
        return result;
    }
}