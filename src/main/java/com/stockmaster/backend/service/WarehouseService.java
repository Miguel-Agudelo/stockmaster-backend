package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.ProductStockDto;
import com.stockmaster.backend.dto.WarehouseDto;
import com.stockmaster.backend.dto.WarehouseListDto;
import com.stockmaster.backend.dto.WarehouseSelectionDto;
import com.stockmaster.backend.entity.Warehouse;
import com.stockmaster.backend.repository.InventoryRepository;
import com.stockmaster.backend.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WarehouseService {

    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private InventoryRepository inventoryRepository;

    // HU12 - Registro de almacenes
    @Transactional
    public Warehouse createWarehouse(WarehouseDto dto) {
        if (dto.getName() == null || dto.getName().isEmpty()) {
            throw new IllegalArgumentException("El nombre del almacén es obligatorio.");
        }
        if (warehouseRepository.findByName(dto.getName()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un almacén con ese nombre.");
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setName(dto.getName());
        warehouse.setAddress(dto.getAddress());
        warehouse.setCity(dto.getCity());
        warehouse.setDescription(dto.getDescription());
        warehouse.setActive(true); // ✅ CORREGIDO: Usando setActive()
        return warehouseRepository.save(warehouse);
    }

    // HU11 - Actualización de almacenes
    @Transactional
    public Warehouse updateWarehouse(Long id, WarehouseDto dto) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .filter(Warehouse::isActive) // ✅ CORREGIDO: Usando isActive() (o getIsActive() si lo tiene)
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado."));

        // Criterio de Aceptación: Puede modificar el nombre y la ubicación
        warehouse.setName(dto.getName());
        warehouse.setAddress(dto.getAddress());
        warehouse.setCity(dto.getCity());
        warehouse.setDescription(dto.getDescription());
        return warehouseRepository.save(warehouse);
    }

    // HU13 - Eliminación de almacenes
    @Transactional
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .filter(Warehouse::isActive) // ✅ CORREGIDO: Usando isActive() (o getIsActive() si lo tiene)
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado."));

        // Criterio de Aceptación: Si un almacén tiene stock, no se permite la eliminación.
        long stockCount = inventoryRepository.countByWarehouseAndCurrentStockGreaterThan(warehouse, 0);
        if (stockCount > 0) {
            throw new IllegalStateException("El almacén tiene productos asignados y no puede ser eliminado. Productos con stock activo: " + stockCount);
        }
        warehouse.setActive(false); // ✅ CORREGIDO: Usando setActive()
        warehouseRepository.save(warehouse);
    }

    // HU10 - Visualización de almacenes (Lista completa con stock total)
    public List<WarehouseListDto> getAllWarehouses() {
        // Asumo que tu consulta JPQL en WarehouseRepository usa 'w.isActive' o 'w.active'
        List<Object[]> results = warehouseRepository.findAllActiveWarehousesWithTotalStock();

        return results.stream()
                .map(result -> {
                    WarehouseListDto dto = new WarehouseListDto();
                    dto.setId((Long) result[0]);
                    dto.setName((String) result[1]);
                    dto.setAddress((String) result[2]);
                    dto.setCity((String) result[3]);
                    dto.setDescription((String) result[4]);
                    Long totalStockLong = (Long) result[5];
                    dto.setTotalStock(totalStockLong != null ? totalStockLong.intValue() : 0);

                    // Obtener productos y stock específicos del almacén
                    // Esto asume que el método findById() retorna un Optional<Warehouse>
                    Warehouse warehouse = warehouseRepository.findById(dto.getId()).orElse(null);

                    List<ProductStockDto> products = (warehouse != null)
                            ? inventoryRepository.findByWarehouseAndCurrentStockGreaterThan(warehouse, 0)
                            .stream()
                            .map(inventory -> {
                                ProductStockDto prodDto = new ProductStockDto();
                                prodDto.setProductId(inventory.getProduct().getId());
                                prodDto.setProductName(inventory.getProduct().getName());
                                prodDto.setCurrentStock(inventory.getCurrentStock());
                                prodDto.setMinStock(inventory.getMinStock());
                                return prodDto;
                            })
                            .collect(Collectors.toList())
                            : List.of(); // Lista vacía si no se encuentra el almacén

                    dto.setProducts(products);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // Método para obtener almacenes activos para selección (lista simple)
    public List<WarehouseSelectionDto> getActiveWarehousesForSelection() {
        return warehouseRepository.findActiveWarehousesForSelection()
                .stream()
                .map(wh -> new WarehouseSelectionDto(wh.getId(), wh.getName()))
                .collect(Collectors.toList());
    }
}
