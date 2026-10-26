
package com.stockflow.api.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SupplierProductRequest(

    @NotNull(message = "Le fournisseur est obligatoire")
    @Positive(message = "L'identifiant du fournisseur doit être positif")
    Long supplierId,

    @Size(max = 100, message = "La référence fournisseur ne doit pas dépasser 100 caractères")
    String supplierReference,

    @NotNull(message = "Le prix d'achat est obligatoire")
    @DecimalMin(value = "0.00", message = "Le prix d'achat ne peut pas être négatif")
    BigDecimal unitPrice

) {}
