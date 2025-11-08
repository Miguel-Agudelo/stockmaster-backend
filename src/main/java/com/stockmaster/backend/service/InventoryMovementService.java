package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.MovementDto;
import com.stockmaster.backend.entity.*;
import com.stockmaster.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List; //  ¡IMPORT NECESARIO!
import java.util.stream.Collectors; //  ¡IMPORT NECESARIO!

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

    //  NUEVO MÉTODO: OBTENER HISTORIAL DE MOVIMIENTOS
    // =========================================================
    public List<MovementDto> getAllMovements() {
        // 1. Obtener todas las entidades del repositorio
        List<InventoryMovement> movements = movementRepository.findAll();

        // 2. Mapear cada entidad a un MovementDto
        return movements.stream()
                .map(this::convertToDto) // Usa el nuevo método de mapeo
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
                        // HU09: No se puede sacar stock si no existe el registro de inventario (stock inicial es 0)
                        throw new IllegalStateException("No existe inventario de este producto en el almacén seleccionado.");
                    }
                    Inventory newInventory = new Inventory();
                    newInventory.setProduct(product);
                    newInventory.setWarehouse(warehouse);
                    newInventory.setCurrentStock(0);
                    // Se asigna un valor por defecto de 10.
                    // El campo minStock se define en Inventory, no en Product.
                    newInventory.setMinStock(10);
                    return newInventory;
                });

        // 3. Aplicar lógica de actualización de stock (HU08 y HU09)
        int newStock;
        if (type.equals("ENTRADA")) {
            newStock = inventory.getCurrentStock() + dto.getQuantity();
        } else { // SALIDA
            if (inventory.getCurrentStock() < dto.getQuantity()) {
                throw new IllegalStateException("Stock insuficiente. Hay " + inventory.getCurrentStock() + " unidades y se intenta sacar " + dto.getQuantity() + ".");
            }
            newStock = inventory.getCurrentStock() - dto.getQuantity();
        }
        // 4. Guardar el nuevo stock
        inventory.setCurrentStock(newStock);
        inventoryRepository.save(inventory);
        // 5. Registrar el movimiento (trazabilidad)
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

        dto.setProductId(movement.getProduct().getId());
        dto.setProductName(movement.getProduct().getName());
        dto.setWarehouseId(movement.getWarehouse().getId());
        dto.setWarehouseName(movement.getWarehouse().getName());
        dto.setQuantity(movement.getQuantity());
        dto.setMotive(movement.getMotive());
        dto.setMovementType(movement.getMovementType());
        dto.setUserId(movement.getUser().getId());
        dto.setUserName(movement.getUser().getName());
        dto.setMovementDate(movement.getMovementDate());


        // Producto
        if (movement.getProduct() != null) {
            dto.setProductId(movement.getProduct().getId());
            dto.setProductName(movement.getProduct().getName()); // 🟢 ESTO ES LO CRUCIAL
        }

        // Almacén/Bodega
        if (movement.getWarehouse() != null) {
            dto.setWarehouseId(movement.getWarehouse().getId());
            dto.setWarehouseName(movement.getWarehouse().getName()); // 🟢 ESTO ES LO CRUCIAL
        }

        // Usuario
        if (movement.getUser() != null) {
            dto.setUserId(movement.getUser().getId());
            // Asumiendo que tu entidad User tiene un método getName() o getEmail()
            dto.setUserName(movement.getUser().getName()); // 🟢 ESTO ES LO CRUCIAL
        }
        return dto;
    }
}