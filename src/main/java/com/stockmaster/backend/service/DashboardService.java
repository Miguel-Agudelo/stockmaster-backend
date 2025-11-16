package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.DashboardMetricDto;
import com.stockmaster.backend.dto.LowStockProductDto;
import com.stockmaster.backend.dto.RecentMovementDto;
import com.stockmaster.backend.entity.Inventory;
import com.stockmaster.backend.entity.InventoryMovement;
import com.stockmaster.backend.repository.InventoryMovementRepository;
import com.stockmaster.backend.repository.InventoryRepository;
import com.stockmaster.backend.repository.ProductRepository;
import com.stockmaster.backend.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.stockmaster.backend.repository.UserRepository;

@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final InventoryRepository inventoryRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    public DashboardService(ProductRepository productRepository, WarehouseRepository warehouseRepository,
                            InventoryMovementRepository inventoryMovementRepository, InventoryRepository inventoryRepository,
                            UserRepository userRepository) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Reúne todos los datos de resumen necesarios para la vista del Dashboard.
     * @param userName Nombre del usuario logueado.
     * @return DashboardMetricDto con todas las métricas.
     */
    @Transactional(readOnly = true)
    public DashboardMetricDto getDashboardSummary(String userName) {
        DashboardMetricDto summary = new DashboardMetricDto();

        summary.setUserName(userName);

        // Contar productos activos (Usamos el metodo count de JpaRepository)
        long totalProducts = productRepository.countByIsActiveTrue();
        summary.setTotalProducts(totalProducts);

        // Contar almacenes activos (Usamos el metodo count de JpaRepository)
        long totalWarehouses = warehouseRepository.countByIsActiveTrue();
        summary.setTotalWarehouses(totalWarehouses);

        // Implementar cálculo del stock total del inventario (Nueva consulta necesaria)
        summary.setTotalStock(calculateTotalStock()); // Llamada a un nuevo metodo privado

        // Contar movimientos hoy
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        long movementsToday = inventoryMovementRepository.countByMovementDateBetween(startOfDay, endOfDay);
        summary.setMovementsToday(movementsToday);


        // Alertas de Stock Bajo (InventoryRepository) ---
        List<LowStockProductDto> lowStockProducts = getLowStockAlerts();
        summary.setLowStockProducts(lowStockProducts);
        summary.setLowStockCount(lowStockProducts.size());


        // Movimientos Recientes (InventoryMovementRepository) ---
        List<RecentMovementDto> recentMovements = getRecentMovements(5); // Obtener los últimos 5
        summary.setRecentMovements(recentMovements);

        summary.setTotalUsers(userRepository.count());
        summary.setTotalMovements(inventoryMovementRepository.count());
        return summary;
    }

    /**
     * Calcula el stock total sumando el currentStock de todas las entradas de Inventory.
     */
    private long calculateTotalStock() {
        try {
            Long totalStock = inventoryRepository.calculateTotalStock();
            return totalStock != null ? totalStock : 0;
        } catch (Exception e) {
            return 0;
        }
    }


    /**
     * Obtiene los productos cuyo stock actual es menor a su stock mínimo.
     */
    private List<LowStockProductDto> getLowStockAlerts() {
        List<Inventory> lowStockItems = inventoryRepository.findItemsWithLowStock();
        return lowStockItems.stream()
                .map(item -> new LowStockProductDto(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getWarehouse().getName(),
                        item.getCurrentStock(),
                        item.getMinStock()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los N movimientos más recientes y los mapea a DTO.
     */
    private List<RecentMovementDto> getRecentMovements(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<InventoryMovement> movements = inventoryMovementRepository.findByOrderByMovementDateDesc(pageable);

        return movements.stream()
                .map(m -> new RecentMovementDto(
                        m.getId(),
                        m.getProduct().getName(),
                        m.getWarehouse().getName(),
                        m.getQuantity() * (m.getMovementType().equals("SALIDA") ? -1 : 1),
                        m.getMovementDate().toLocalDate(),
                        m.getUser() != null ? m.getUser().getName() : "Sistema"
                ))
                .collect(Collectors.toList());
    }
}