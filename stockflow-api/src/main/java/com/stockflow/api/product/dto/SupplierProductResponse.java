
package com.stockflow.api.product.dto;

import java.math.BigDecimal;

public record SupplierProductResponse(
    Long supplierId,
    String supplierName,
    String supplierReference,
    BigDecimal unitPrice
) {}
