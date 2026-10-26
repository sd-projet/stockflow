
package com.stockflow.api.product.dto;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
    Long id,
    String reference,
    String name,
    String description,
    Long categoryId,
    String categoryName,
    Integer quantityInStock,
    Integer minimumStock,
    String unit,
    BigDecimal unitPrice,
    Boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<SupplierProductResponse> suppliers
) {}
