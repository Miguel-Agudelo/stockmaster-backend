package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.MovementDto;
import com.stockmaster.backend.dto.StockAdjustmentDto;
import com.stockmaster.backend.dto.TransferDto;
import com.stockmaster.backend.dto.WarehouseStockDto;
import com.stockmaster.backend.entity.*;
import com.stockmaster.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public List<MovementDto> getAllMovements() {
        List<InventoryMovement> movements = movementRepository.findAll();
        return movements.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<WarehouseStockDto> getProductStockByWarehouses(Long productId) {
        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        return inventories.stream()
                .filter(i -> i.getCurrentStock() > 0)
                .map(inventory -> new WarehouseStockDto(
                        inventory.getWarehouse().getId(),
                        inventory.getCurrentStock()
                ))
                .collect(Collectors.toList());
    }

    /**
     * HU11 — Retorna el stock actual de un producto en un almacén específico.
     * Usado por el frontend para mostrar "Stock actual" en tiempo real.
     */
    public int getCurrentStock(Long productId, Long warehouseId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado."));
        return inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .map(Inventory::getCurrentStock)
                .orElse(0);
    }

    @Transactional
    public InventoryMovement registerEntry(MovementDto dto) {
        return registerMovement(dto, "ENTRADA");
    }

    @Transactional
    public InventoryMovement registerExit(MovementDto dto) {
        return registerMovement(dto, "SALIDA");
    }

    private InventoryMovement registerMovement(MovementDto dto, String type) {
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

        Inventory inventory = inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .orElseGet(() -> {
                    if (type.equals("SALIDA")) {
                        throw new IllegalStateException("No existe inventario de este producto en el almacén seleccionado.");
                    }
                    Inventory newInventory = new Inventory();
                    newInventory.setProduct(product);
                    newInventory.setWarehouse(warehouse);
                    newInventory.setCurrentStock(0);
                    newInventory.setMinStock(12);
                    return newInventory;
                });

        int newStock;
        if (type.equals("ENTRADA")) {
            newStock = inventory.getCurrentStock() + dto.getQuantity();
        } else {
            if (inventory.getCurrentStock() < dto.getQuantity()) {
                throw new IllegalStateException("Stock insuficiente. Hay " + inventory.getCurrentStock() + " unidades y se intenta sacar " + dto.getQuantity() + ".");
            }
            newStock = inventory.getCurrentStock() - dto.getQuantity();
        }
        inventory.setCurrentStock(newStock);
        inventoryRepository.save(inventory);

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setQuantity(dto.getQuantity());
        movement.setMovementType(type);
        movement.setUser(user);
        movement.setMotive(dto.getMotive());

        return movementRepository.save(movement);
    }

    private MovementDto convertToDto(InventoryMovement movement) {
        MovementDto dto = new MovementDto();
        dto.setId(movement.getId());
        dto.setQuantity(movement.getQuantity());
        dto.setMovementType(movement.getMovementType());
        dto.setMovementDate(movement.getMovementDate());
        dto.setMotive(movement.getMotive());
        dto.setTransferReference(movement.getTransferReference());

        if (movement.getProduct() != null) {
            dto.setProductId(movement.getProduct().getId());
            dto.setProductName(movement.getProduct().getName());
        }
        if (movement.getWarehouse() != null) {
            dto.setWarehouseId(movement.getWarehouse().getId());
            dto.setWarehouseName(movement.getWarehouse().getName());
        }
        if (movement.getUser() != null) {
            dto.setUserId(movement.getUser().getId());
            dto.setUserName(movement.getUser().getName());
        }
        return dto;
    }

    // HU20 - Transferencia de Stock
    @Transactional
    public Map<String, Object> transferStock(TransferDto dto) {
        if (dto.getOriginWarehouseId().equals(dto.getDestinationWarehouseId())) {
            throw new IllegalArgumentException("El almacén de origen y destino no pueden ser el mismo.");
        }
        if (dto.getQuantity() <= 0) {
            throw new IllegalArgumentException("La cantidad a transferir debe ser mayor a cero.");
        }

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

        String transferReference = "TRANS-" + System.currentTimeMillis();

        Inventory originInventory = getAndValidateInventory(product, origin, dto.getQuantity(), "SALIDA");
        Inventory destinationInventory = getAndValidateInventory(product, destination, 0, "ENTRADA");

        InventoryMovement exitMovement = executeMovement(product, origin, user, dto.getQuantity(),
                "SALIDA", transferReference, dto.getMotive(), originInventory);
        InventoryMovement entryMovement = executeMovement(product, destination, user, dto.getQuantity(),
                "ENTRADA", transferReference, dto.getMotive(), destinationInventory);

        Map<String, Object> result = new HashMap<>();
        result.put("transferReference", transferReference);
        result.put("exitMovement", exitMovement);
        result.put("entryMovement", entryMovement);
        return result;
    }

    private Inventory getAndValidateInventory(Product product, Warehouse warehouse, int quantity, String type) {
        Inventory inventory = inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .orElseGet(() -> {
                    if (type.equals("SALIDA")) {
                        throw new IllegalStateException("No existe inventario de este producto en el almacén seleccionado.");
                    }
                    Inventory newInventory = new Inventory();
                    newInventory.setProduct(product);
                    newInventory.setWarehouse(warehouse);
                    newInventory.setCurrentStock(0);
                    newInventory.setMinStock(12);
                    return newInventory;
                });

        if (type.equals("SALIDA") && inventory.getCurrentStock() < quantity) {
            throw new IllegalStateException("Stock insuficiente en el almacén de origen. Hay " + inventory.getCurrentStock() + " unidades y se intenta sacar " + quantity + ".");
        }
        return inventory;
    }

    private InventoryMovement executeMovement(Product product, Warehouse warehouse, User user, int quantity,
                                              String type, String transferRef, String motive, Inventory inventory) {
        if (inventory.getId() == null) {
            inventoryRepository.save(inventory);
        }

        int newStock = type.equals("ENTRADA")
                ? inventory.getCurrentStock() + quantity
                : inventory.getCurrentStock() - quantity;

        inventory.setCurrentStock(newStock);
        inventoryRepository.save(inventory);

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setQuantity(quantity);
        movement.setMovementType(type);
        movement.setUser(user);
        movement.setTransferReference(transferRef);
        movement.setMotive(motive);

        return movementRepository.save(movement);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HU11 — Ajuste de inventario
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Corrige discrepancias de stock de forma manual y justificada.
     * Registra un movimiento tipo AJUSTE_POSITIVO o AJUSTE_NEGATIVO.
     */
    @Transactional
    public InventoryMovement registerStockAdjustment(StockAdjustmentDto dto) {

        // 1. Validaciones básicas
        if (dto.getQuantity() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }
        if (dto.getReason() == null || dto.getReason().isBlank()) {
            throw new IllegalArgumentException("La razón del ajuste es obligatoria.");
        }
        if (!"POSITIVO".equals(dto.getAdjustmentType()) && !"NEGATIVO".equals(dto.getAdjustmentType())) {
            throw new IllegalArgumentException("El tipo de ajuste debe ser POSITIVO o NEGATIVO.");
        }

        // 2. Validar entidades
        Product product = productRepository.findById(dto.getProductId())
                .filter(Product::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado o inactivo."));
        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado."));
        User user = userRepository.findById(dto.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado o inactivo."));

        // 3. Obtener o crear registro de inventario
        Inventory inventory = inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .orElseGet(() -> {
                    if ("NEGATIVO".equals(dto.getAdjustmentType())) {
                        throw new IllegalStateException(
                                "No existe inventario de este producto en el almacén seleccionado.");
                    }
                    Inventory newInventory = new Inventory();
                    newInventory.setProduct(product);
                    newInventory.setWarehouse(warehouse);
                    newInventory.setCurrentStock(0);
                    newInventory.setMinStock(12);
                    return newInventory;
                });

        // 4. Calcular nuevo stock
        int newStock;
        String movementType;
        if ("POSITIVO".equals(dto.getAdjustmentType())) {
            newStock     = inventory.getCurrentStock() + dto.getQuantity();
            movementType = "AJUSTE_POSITIVO";
        } else {
            if (inventory.getCurrentStock() < dto.getQuantity()) {
                throw new IllegalStateException(
                        "Stock insuficiente. Hay " + inventory.getCurrentStock()
                                + " unidades y se intenta ajustar -" + dto.getQuantity() + ".");
            }
            newStock     = inventory.getCurrentStock() - dto.getQuantity();
            movementType = "AJUSTE_NEGATIVO";
        }

        // 5. Persistir stock actualizado
        inventory.setCurrentStock(newStock);
        inventoryRepository.save(inventory);

        // 6. Registrar el movimiento de ajuste
        String motive = dto.getReason();
        if (dto.getNotes() != null && !dto.getNotes().isBlank()) {
            motive = motive + ": " + dto.getNotes().trim();
        }

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setQuantity(dto.getQuantity());
        movement.setMovementType(movementType);
        movement.setUser(user);
        movement.setMotive(motive);

        return movementRepository.save(movement);
    }
}
