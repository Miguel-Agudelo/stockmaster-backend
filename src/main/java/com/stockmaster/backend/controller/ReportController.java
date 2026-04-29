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
import java.util.List;
import java.time.LocalDate;

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
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<SupplierTraceabilityDto>> getSupplierTraceabilityReport(
            @RequestParam Long supplierId) {
        return ResponseEntity.ok(reportService.getSupplierTraceabilityReport(supplierId));
    }

    // HU-PI2-09 - Exportar Excel de Trazabilidad por Proveedor
    @GetMapping("/supplier-traceability/export/excel")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<byte[]> exportSupplierTraceabilityExcel(@RequestParam Long supplierId) {
        byte[] excelBytes = reportService.exportSupplierTraceabilityToExcel(supplierId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=trazabilidad_proveedor.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excelBytes);
    }
}
