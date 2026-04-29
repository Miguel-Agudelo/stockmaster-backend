package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.MovementReportDto;
import com.stockmaster.backend.dto.SalesReportDto;
import com.stockmaster.backend.dto.StockReportDto;
import com.stockmaster.backend.dto.SupplierTraceabilityDto;
import com.stockmaster.backend.entity.InventoryMovement;
import com.stockmaster.backend.repository.InventoryMovementRepository;
import com.stockmaster.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stockmaster.backend.entity.Inventory;
import com.stockmaster.backend.repository.InventoryRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

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
                    dto.setProductId(inventory.getProduct().getId());
                    dto.setProductName(inventory.getProduct().getName());
                    dto.setWarehouseName(inventory.getWarehouse().getName());
                    dto.setCurrentStock(inventory.getCurrentStock());
                    dto.setMinimumStock(inventory.getMinStock());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // HU15: Reporte de Movimientos por Fecha
    public List<MovementReportDto> getMovementReportByDate(LocalDate startDate, LocalDate endDate) {
        LocalDateTime finalStartDate = startDate.atStartOfDay();
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
                    dto.setUserName(m.getUser().getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // HU16: Reporte de Productos Más Vendidos
    public List<SalesReportDto> getMostSoldProductsReport() {
        List<Object[]> results = inventoryMovementRepository.findMostSoldProductsWithRevenueAndAveragePrice();

        return results.stream()
                .map(result -> {
                    SalesReportDto dto = new SalesReportDto();
                    dto.setProductId((Long) result[0]);
                    dto.setProductName((String) result[1]);
                    dto.setUnitsSold((Long) result[2]);
                    dto.setTotalRevenue((Double) result[3]);
                    dto.setAveragePrice((Double) result[4]);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // HU-PI2-09: Reporte de Trazabilidad por Proveedor
    public List<SupplierTraceabilityDto> getSupplierTraceabilityReport(Long supplierId) {
        List<Inventory> items = inventoryRepository.findBySupplierId(supplierId);

        return items.stream()
                .map(inventory -> {
                    SupplierTraceabilityDto dto = new SupplierTraceabilityDto();
                    dto.setProductName(inventory.getProduct().getName());
                    dto.setCategoryName(inventory.getProduct().getCategory().getName());
                    dto.setTotalStock(inventory.getCurrentStock());
                    dto.setWarehouseName(inventory.getWarehouse().getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // HU-PI2-09: Exportar Trazabilidad a Excel
    public byte[] exportSupplierTraceabilityToExcel(Long supplierId) {
        List<SupplierTraceabilityDto> data = getSupplierTraceabilityReport(supplierId);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Trazabilidad Proveedor");

            Row header = sheet.createRow(0);
            String[] columns = {"Producto", "Categoría", "Stock Total", "Almacén"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int rowNum = 1;
            for (SupplierTraceabilityDto dto : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dto.getProductName());
                row.createCell(1).setCellValue(dto.getCategoryName());
                row.createCell(2).setCellValue(dto.getTotalStock());
                row.createCell(3).setCellValue(dto.getWarehouseName());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el archivo Excel", e);
        }
    }
}
