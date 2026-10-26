package com.stockflow.api.dashboard.dto;

import com.stockflow.api.stock.dto.StockMovementResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        long activeProducts,
        BigDecimal stockValue,
        long lowStockCount,
        long outOfStockCount,
        long draftOrders,
        long openOrders,
        Map<String, Long> ordersByStatus,
        List<LowStockItem> lowStockProducts,
        List<StockMovementResponse> recentMovements
) {

    public record LowStockItem(
            Long id,
            String reference,
            String name,
            Integer quantityInStock,
            Integer minimumStock,
            String unit
    ) {
    }
}
