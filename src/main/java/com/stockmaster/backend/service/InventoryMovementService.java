package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.MovementDto;
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
        // 1. Obtener todas las entidades del repositorio
        List<InventoryMovement> movements = movementRepository.findAll();

        // 2. Mapear cada entidad a un MovementDto
        return movements.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<WarehouseStockDto> getProductStockByWarehouses(Long productId) {
        // 1. Obtener la lista de Inventarios para ese producto
        List<Inventory> inventories = inventoryRepository.findByProductId(productId);

        // 2. Mapear cada entidad Inventory a un WarehouseStockDto
        return inventories.stream()
                .filter(i -> i.getCurrentStock() > 0)
                .map(inventory -> new WarehouseStockDto(
                        inventory.getWarehouse().getId(),
                        inventory.getCurrentStock()
                ))
                .collect(Collectors.toList());
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
        // 1. Validar entidades
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

        // 2. Obtener o crear el registro de Inventario (stock)
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

        // 3. Aplicar lógica de actualización de stock
        int newStock;
        if (type.equals("ENTRADA")) {
            newStock = inventory.getCurrentStock() + dto.getQuantity();
        } else {
            if (inventory.getCurrentStock() < dto.getQuantity()) {
                throw new IllegalStateException("Stock insuficiente. Hay " + inventory.getCurrentStock() + " unidades y se intenta sacar " + dto.getQuantity() + ".");
            }
            newStock = inventory.getCurrentStock() - dto.getQuantity();
        }
        // 4. Guardar el nuevo stock
        inventory.setCurrentStock(newStock);
        inventoryRepository.save(inventory);

        // 5. Registrar el movimiento
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

        // Producto
        if (movement.getProduct() != null) {
            dto.setProductId(movement.getProduct().getId());
            dto.setProductName(movement.getProduct().getName());
        }

        // Almacén/Bodega
        if (movement.getWarehouse() != null) {
            dto.setWarehouseId(movement.getWarehouse().getId());
            dto.setWarehouseName(movement.getWarehouse().getName());
        }

        // Usuario
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

        // 2. Generar Referencia Única de Transferencia
        String transferReference = "TRANS-" + System.currentTimeMillis();

        // 3. OBTENER INVENTARIO Y VERIFICAR STOCK
        Inventory originInventory = getAndValidateInventory(product, origin, dto.getQuantity(), "SALIDA");
        Inventory destinationInventory = getAndValidateInventory(product, destination, 0, "ENTRADA");

        // 4. EJECUTAR SALIDA (DEL ORIGEN) - Primer paso transaccional
        InventoryMovement exitMovement = executeMovement(product, origin, user, dto.getQuantity(),
                "SALIDA", transferReference, dto.getMotive(), originInventory);

        // 5. EJECUTAR ENTRADA (AL DESTINO) - Segundo paso transaccional
        InventoryMovement entryMovement = executeMovement(product, destination, user, dto.getQuantity(),
                "ENTRADA", transferReference, dto.getMotive(), destinationInventory);

        // 6. Retornar resultados
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

        // 1. Cálculo del Nuevo Stock
        int newStock = type.equals("ENTRADA")
                ? inventory.getCurrentStock() + quantity
                : inventory.getCurrentStock() - quantity;

        // 2. Actualización y Persistencia de Stock
        inventory.setCurrentStock(newStock);
        inventoryRepository.save(inventory);

        // 3. Registro del Movimiento
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
}