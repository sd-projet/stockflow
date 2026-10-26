package com.stockflow.api.supplier.dto;

import java.time.OffsetDateTime;

public record SupplierResponse(
        Long id,
        String name,
        String contactName,
        String email,
        String phone,
        String address,
        String city,
        String postalCode,
        String country,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
