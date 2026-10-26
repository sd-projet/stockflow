
package com.stockflow.api.purchaseorder.dto;

import java.math.BigDecimal;

public record PurchaseOrderLineResponse(
    Long id,
    Long productId,
    String productName,
    Integer quantityOrdered,
    Integer quantityReceived,
    BigDecimal unitPrice,
    BigDecimal totalPrice
) {}
