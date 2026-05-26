package com.stockmaster.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardMetricDto {
    private long totalProducts;
    private long totalWarehouses;
    private long totalStock;
    private long movementsToday;
    private long lowStockCount;
    private long totalUsers;
    private long totalMovements;
    private List<LowStockProductDto> lowStockProducts;
    private List<RecentMovementDto> recentMovements;
    private String userName;

    // HU-PI2-03: Tablero de control
    private List<WarehouseStockChartDto> warehouseStockChart;
    private List<CategoryStockChartDto> categoryStockChart;
    private double totalInventoryValue;

    // Dashboard mejorado
    private double totalRevenue;
    private List<MonthlyMovementsDto> movimientosMensuales;
    private List<TopProductStockDto> topProductosByStock;
}