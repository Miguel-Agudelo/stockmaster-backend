package com.stockmaster.backend.controller;

import com.stockmaster.backend.dto.MovementReportDto;
import com.stockmaster.backend.dto.SalesReportDto;
import com.stockmaster.backend.dto.StockReportDto;
import com.stockmaster.backend.dto.SupplierTraceabilityDto;
import com.stockmaster.backend.service.ReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
public class ReportController {

    @Autowired
    private ReportService reportService;


    // HU14 - Reporte de stock bajo
    @GetMapping("/low-stock")
    public ResponseEntity<List<StockReportDto>> getLowStockReport() {
        return ResponseEntity.ok(reportService.getLowStockReport());
    }

    // HU15 - Reporte de movimientos de inventario por fecha
    @GetMapping("/movements")
    public ResponseEntity<List<MovementReportDto>> getMovementReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(reportService.getMovementReportByDate(startDate, endDate));
    }

    // HU16 - Reporte de productos más vendidos
    @GetMapping("/sales")
    public ResponseEntity<List<SalesReportDto>> getMostSoldProductsReport() {
        return ResponseEntity.ok(reportService.getMostSoldProductsReport());
    }

    // HU-PI2-09 - Reporte de Trazabilidad por Proveedor
    @GetMapping("/supplier-traceability")
    public ResponseEntity<List<SupplierTraceabilityDto>> getSupplierTraceabilityReport(
            @RequestParam Long supplierId) {
        return ResponseEntity.ok(reportService.getSupplierTraceabilityReport(supplierId));
    }

    // HU-PI2-05 | HU14: Exportar Stock Bajo → Excel
    @GetMapping("/low-stock/export/excel")
    public ResponseEntity<byte[]> exportLowStockExcel() {
        List<StockReportDto> data = reportService.getLowStockReport();
        byte[] excelBytes = reportService.exportLowStockToExcel(data);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=reporte_stock_bajo.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excelBytes);
    }

    // HU-PI2-05 | HU15: Exportar Movimientos → Excel
    @GetMapping("/movements/export/excel")
    public ResponseEntity<byte[]> exportMovementsExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        List<MovementReportDto> data = reportService.getMovementReportByDate(startDate, endDate);
        byte[] excelBytes = reportService.exportMovementsToExcel(data, startDate, endDate);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=reporte_movimientos.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excelBytes);
    }

    // HU-PI2-05 | HU16: Exportar Más Vendidos → Excel
    @GetMapping("/sales/export/excel")
    public ResponseEntity<byte[]> exportTopSellingExcel() {
        List<SalesReportDto> data = reportService.getMostSoldProductsReport();
        byte[] excelBytes = reportService.exportTopSellingToExcel(data);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=reporte_mas_vendidos.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excelBytes);
    }

    // HU-PI2-09 (mejorado) + HU-PI2-05: Exportar Trazabilidad Proveedor → Excel
    @GetMapping("/supplier-traceability/export/excel")
    public ResponseEntity<byte[]> exportSupplierTraceabilityExcel(
            @RequestParam Long supplierId,
            @RequestParam(required = false, defaultValue = "") String supplierName) {

        List<SupplierTraceabilityDto> data = reportService.getSupplierTraceabilityReport(supplierId);
        String nameForExcel = supplierName.isBlank() ? "Proveedor #" + supplierId : supplierName;
        byte[] excelBytes = reportService.exportSupplierTraceabilityToExcel(data, nameForExcel);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=reporte_trazabilidad_proveedor.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excelBytes);
    }
}
