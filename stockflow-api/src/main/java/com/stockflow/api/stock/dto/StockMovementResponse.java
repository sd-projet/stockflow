
package com.stockflow.api.stock.dto;

import com.stockflow.api.stock.MovementType;
import java.time.OffsetDateTime;

public record StockMovementResponse(
        Long id,
        Long productId,
        String productReference,
        String productName,
        MovementType type,
        Integer quantityBefore,
        Integer quantityAfter,
        String reason,
        Long purchaseOrderLineId,
        Long performedBy,
        OffsetDateTime createdAt
) {
}
