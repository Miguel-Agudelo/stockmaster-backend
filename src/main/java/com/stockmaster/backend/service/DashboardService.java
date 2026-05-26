package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.*;
import com.stockmaster.backend.entity.Inventory;
import com.stockmaster.backend.entity.InventoryMovement;
import com.stockmaster.backend.repository.InventoryMovementRepository;
import com.stockmaster.backend.repository.InventoryRepository;
import com.stockmaster.backend.repository.ProductRepository;
import com.stockmaster.backend.repository.WarehouseRepository;
import com.stockmaster.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final InventoryRepository inventoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    public DashboardService(ProductRepository productRepository,
                            WarehouseRepository warehouseRepository,
                            InventoryMovementRepository inventoryMovementRepository,
                            InventoryRepository inventoryRepository,
                            UserRepository userRepository) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardMetricDto getDashboardSummary(String userName) {
        DashboardMetricDto summary = new DashboardMetricDto();

        summary.setUserName(userName);
        summary.setTotalProducts(productRepository.countByIsActiveTrue());
        summary.setTotalWarehouses(warehouseRepository.countByIsActiveTrue());
        summary.setTotalStock(calculateTotalStock());

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = LocalDate.now().atTime(LocalTime.MAX);
        summary.setMovementsToday(inventoryMovementRepository.countByMovementDateBetween(startOfDay, endOfDay));

        List<LowStockProductDto> lowStockProducts = getLowStockAlerts();
        summary.setLowStockProducts(lowStockProducts);
        summary.setLowStockCount(lowStockProducts.size());

        summary.setRecentMovements(getRecentMovements(5));
        summary.setTotalUsers(userRepository.count());
        summary.setTotalMovements(inventoryMovementRepository.count());

        // HU-PI2-03
        summary.setWarehouseStockChart(getWarehouseStockChart());
        summary.setCategoryStockChart(getCategoryStockChart());
        summary.setTotalInventoryValue(calculateTotalInventoryValue());

        // Dashboard mejorado
        summary.setTotalRevenue(calculateTotalRevenue());
        summary.setMovimientosMensuales(getMovimientosMensuales());
        summary.setTopProductosByStock(getTopProductosByStock());

        return summary;
    }

    private long calculateTotalStock() {
        try {
            Long v = inventoryRepository.calculateTotalStock();
            return v != null ? v : 0;
        } catch (Exception e) { return 0; }
    }

    private List<LowStockProductDto> getLowStockAlerts() {
        return inventoryRepository.findItemsWithLowStock().stream()
                .map(item -> new LowStockProductDto(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getWarehouse().getName(),
                        item.getCurrentStock(),
                        item.getMinStock()
                ))
                .collect(Collectors.toList());
    }

    private List<RecentMovementDto> getRecentMovements(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return inventoryMovementRepository.findByOrderByMovementDateDesc(pageable).stream()
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

    private List<WarehouseStockChartDto> getWarehouseStockChart() {
        try { return inventoryRepository.findStockByActiveWarehouse(); }
        catch (Exception e) { return List.of(); }
    }

    private List<CategoryStockChartDto> getCategoryStockChart() {
        try { return inventoryRepository.findProductCountByCategory(); }
        catch (Exception e) { return List.of(); }
    }

    private double calculateTotalInventoryValue() {
        try {
            Double v = inventoryRepository.calculateTotalInventoryValue();
            return v != null ? v : 0.0;
        } catch (Exception e) { return 0.0; }
    }

    private double calculateTotalRevenue() {
        try {
            Double v = inventoryMovementRepository.calculateTotalRevenue();
            return v != null ? v : 0.0;
        } catch (Exception e) { return 0.0; }
    }

    private List<MonthlyMovementsDto> getMovimientosMensuales() {
        try {
            return inventoryMovementRepository.findMovimientosMensualesAnioActual()
                    .stream()
                    .map(row -> new MonthlyMovementsDto(
                            ((Number) row[0]).intValue(),
                            ((Number) row[1]).intValue(),
                            ((Number) row[2]).longValue(),
                            ((Number) row[3]).longValue()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) { return List.of(); }
    }

    private List<TopProductStockDto> getTopProductosByStock() {
        try {
            Pageable top7 = PageRequest.of(0, 7);
            return inventoryRepository.findTopProductosByStock(top7);
        } catch (Exception e) { return List.of(); }
    }
}