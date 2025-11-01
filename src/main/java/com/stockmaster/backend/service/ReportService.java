package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.MovementReportDto;
import com.stockmaster.backend.dto.SalesReportDto;
import com.stockmaster.backend.dto.StockReportDto;
import com.stockmaster.backend.entity.InventoryMovement;
import com.stockmaster.backend.repository.InventoryMovementRepository;
import com.stockmaster.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    // HU14: Reporte de Stock Bajo
    public List<StockReportDto> getLowStockReport() {
        List<Object[]> results = productRepository.findProductsBelowMinStock();

        return results.stream()
                .map(result -> {
                    StockReportDto dto = new StockReportDto();
                    dto.setProductId((Long) result[0]);
                    dto.setProductName((String) result[1]);
                    // SUM(i.currentStock) viene como Long
                    Long totalStockLong = (Long) result[2];
                    dto.setTotalStockActual(totalStockLong != null ? totalStockLong.intValue() : 0);
                    // i.minStock viene como Integer (asumiendo tu entidad Inventory)
                    dto.setMinStockThreshold((Integer) result[3]);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // HU15: Reporte de Movimientos por Fecha
    public List<MovementReportDto> getMovementReportByDate(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime finalEndDate = endDate.withHour(23).withMinute(59).withSecond(59);
        List<InventoryMovement> movements = inventoryMovementRepository.findByMovementDateBetweenOrderByMovementDateAsc(startDate, finalEndDate);

        return movements.stream()
                .map(m -> {
                    MovementReportDto dto = new MovementReportDto();
                    dto.setMovementDate(m.getMovementDate());
                    dto.setProductName(m.getProduct().getName());
                    dto.setMovementType(m.getMovementType());
                    dto.setQuantity(m.getQuantity());
                    dto.setWarehouseName(m.getWarehouse().getName());
                    //dto.setTransferReference(m.getTransferReference());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // HU16: Reporte de Productos Más Vendidos
    public List<SalesReportDto> getMostSoldProductsReport() {
        List<Object[]> results = inventoryMovementRepository.findMostSoldProductsWithRevenue();
        return results.stream()
                .map(result -> {
                    SalesReportDto dto = new SalesReportDto();
                    dto.setProductId((Long) result[0]);
                    dto.setProductName((String) result[1]);
                    dto.setUnitsSold((Long) result[2]);
                    dto.setTotalRevenue((Double) result[3]);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
