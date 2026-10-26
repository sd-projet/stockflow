
package com.stockflow.api.purchaseorder.dto;

import com.stockflow.api.purchaseorder.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        String orderNumber,
        Long supplierId,
        String supplierName,
        PurchaseOrderStatus status,
        LocalDate orderDate,
        LocalDate expectedDeliveryDate,
        LocalDate receivedDate,
        String notes,
        boolean archived,
        Long createdById,
        String createdByName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<PurchaseOrderLineResponse> lines,
        BigDecimal totalAmount) {
}
