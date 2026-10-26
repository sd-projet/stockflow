
package com.stockflow.api.purchaseorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record PurchaseOrderLineRequest(

    @NotNull(message = "Le produit est obligatoire.")
    @Positive(message = "L'identifiant du produit doit être positif.")
    Long productId,

    @NotNull(message = "La quantité commandée est obligatoire.")
    @Positive(message = "La quantité commandée doit être supérieure à zéro.")
    Integer quantityOrdered,

    @NotNull(message = "Le prix unitaire est obligatoire.")
    @DecimalMin(value = "0.00", message = "Le prix unitaire ne peut pas être négatif.")
    @Digits(integer = 10, fraction = 2,
            message = "Le prix unitaire doit avoir au maximum 10 chiffres entiers et 2 décimales.")
    BigDecimal unitPrice
) {}
