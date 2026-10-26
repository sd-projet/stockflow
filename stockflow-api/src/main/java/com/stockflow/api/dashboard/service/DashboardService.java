package com.stockflow.api.dashboard.service;

import com.stockflow.api.dashboard.dto.DashboardResponse;
import com.stockflow.api.dashboard.dto.DashboardResponse.LowStockItem;
import com.stockflow.api.product.Product;
import com.stockflow.api.product.repository.ProductRepository;
import com.stockflow.api.purchaseorder.enums.PurchaseOrderStatus;
import com.stockflow.api.purchaseorder.repository.PurchaseOrderRepository;
import com.stockflow.api.stock.StockMovement;
import com.stockflow.api.stock.dto.StockMovementResponse;
import com.stockflow.api.stock.repository.StockMovementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int LOW_STOCK_LIMIT = 10;

    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockMovementRepository stockMovementRepository;

    public DashboardService(
            ProductRepository productRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public DashboardResponse getDashboard() {
        List<Product> lowStock = productRepository.findLowStockProducts();

        long outOfStock = lowStock.stream()
                .filter(product -> product.getQuantityInStock() == 0)
                .count();

        List<LowStockItem> lowStockItems = lowStock.stream()
                .sorted(Comparator.comparingInt(
                        product -> product.getQuantityInStock()
                                - product.getMinimumStock()))
                .limit(LOW_STOCK_LIMIT)
                .map(product -> new LowStockItem(
                        product.getId(),
                        product.getReference(),
                        product.getName(),
                        product.getQuantityInStock(),
                        product.getMinimumStock(),
                        product.getUnit()))
                .toList();

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (PurchaseOrderStatus status : PurchaseOrderStatus.values()) {
            ordersByStatus.put(
                    status.name(),
                    purchaseOrderRepository.countByStatus(status));
        }

        long openOrders =
                ordersByStatus.get(PurchaseOrderStatus.ORDERED.name())
                        + ordersByStatus.get(
                                PurchaseOrderStatus.PARTIALLY_RECEIVED.name());

        List<StockMovementResponse> recentMovements =
                stockMovementRepository.findTop8ByOrderByCreatedAtDesc()
                        .stream()
                        .map(this::toMovementResponse)
                        .toList();

        BigDecimal stockValue = productRepository.calculateStockValue();

        return new DashboardResponse(
                productRepository.countByActiveTrue(),
                stockValue == null ? BigDecimal.ZERO : stockValue,
                lowStock.size(),
                outOfStock,
                ordersByStatus.get(PurchaseOrderStatus.DRAFT.name()),
                openOrders,
                ordersByStatus,
                lowStockItems,
                recentMovements);
    }

    private StockMovementResponse toMovementResponse(StockMovement movement) {
        Product product = movement.getProduct();

        return new StockMovementResponse(
                movement.getId(),
                product.getId(),
                product.getReference(),
                product.getName(),
                movement.getType(),
                movement.getQuantityBefore(),
                movement.getQuantityAfter(),
                movement.getReason(),
                movement.getPurchaseOrderLineId(),
                movement.getPerformedBy(),
                movement.getCreatedAt());
    }
}
