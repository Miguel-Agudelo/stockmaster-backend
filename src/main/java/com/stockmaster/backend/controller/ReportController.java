package com.stockmaster.backend.controller;

import com.stockmaster.backend.dto.MovementReportDto;
import com.stockmaster.backend.dto.SalesReportDto;
import com.stockmaster.backend.dto.StockReportDto;
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

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMINISTRADOR')")
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

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
}
