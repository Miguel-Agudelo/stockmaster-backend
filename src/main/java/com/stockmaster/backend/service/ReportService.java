package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.MovementReportDto;
import com.stockmaster.backend.dto.SalesReportDto;
import com.stockmaster.backend.dto.StockReportDto;
import com.stockmaster.backend.entity.InventoryMovement;
import com.stockmaster.backend.repository.InventoryMovementRepository;
import com.stockmaster.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stockmaster.backend.entity.Inventory;
import com.stockmaster.backend.repository.InventoryRepository;

import java.time.LocalDateTime;
import java.time.LocalDate; // Import necesario para LocalDate
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    // HU14: Reporte de Stock Bajo
    public List<StockReportDto> getLowStockReport() {
        List<Inventory> lowStockItems = inventoryRepository.findItemsWithLowStock();

        return lowStockItems.stream()
                .map(inventory -> {
                    StockReportDto dto = new StockReportDto();

                    // 1. Datos de Producto
                    dto.setProductId(inventory.getProduct().getId());
                    dto.setProductName(inventory.getProduct().getName());

                    // 2. Datos de Almacén
                    dto.setWarehouseName(inventory.getWarehouse().getName());

                    // 3. Datos de Stock (Usando nombres de campo corregidos en el DTO)
                    dto.setCurrentStock(inventory.getCurrentStock());
                    dto.setMinimumStock(inventory.getMinStock());

                    return dto;
                })
                .collect(Collectors.toList());
    }

    // HU15: Reporte de Movimientos por Fecha
    public List<MovementReportDto> getMovementReportByDate(LocalDate startDate, LocalDate endDate) {

        // Fecha de inicio: Medianoche del día (00:00:00)
        LocalDateTime finalStartDate = startDate.atStartOfDay();

        // Fecha de fin: Final del día (23:59:59)
        LocalDateTime finalEndDate = endDate.atTime(23, 59, 59);

        List<InventoryMovement> movements = inventoryMovementRepository.findByMovementDateBetweenOrderByMovementDateAsc(finalStartDate, finalEndDate);

        return movements.stream()
                .map(m -> {
                    MovementReportDto dto = new MovementReportDto();
                    dto.setMovementDate(m.getMovementDate());
                    dto.setProductName(m.getProduct().getName());
                    dto.setMovementType(m.getMovementType());
                    dto.setQuantity(m.getQuantity());
                    dto.setWarehouseName(m.getWarehouse().getName());
                    //dto.setTransferReference(m.getTransferReference());
                    dto.setUserName(m.getUser().getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // HU16: Reporte de Productos Más Vendidos
    public List<SalesReportDto> getMostSoldProductsReport() {
        // Asumo que findMostSoldProductsWithRevenueAndAveragePrice devuelve 5 campos, como corregimos anteriormente
        List<Object[]> results = inventoryMovementRepository.findMostSoldProductsWithRevenueAndAveragePrice();

        return results.stream()
                .map(result -> {
                    SalesReportDto dto = new SalesReportDto();
                    dto.setProductId((Long) result[0]);
                    dto.setProductName((String) result[1]);
                    dto.setUnitsSold((Long) result[2]);
                    dto.setTotalRevenue((Double) result[3]);
                    // Mapeamos el quinto campo (índice 4)
                    dto.setAveragePrice((Double) result[4]);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}