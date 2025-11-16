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
}