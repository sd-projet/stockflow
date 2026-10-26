
package com.stockflow.api.purchaseorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderUpdateRequest(

    @NotBlank(message = "Le numéro de commande est obligatoire.")
    @Size(max = 50, message = "Le numéro de commande ne doit pas dépasser 50 caractères.")
    String orderNumber,

    @NotNull(message = "Le fournisseur est obligatoire.")
    @Positive(message = "L'identifiant du fournisseur doit être positif.")
    Long supplierId,

    LocalDate orderDate,

    LocalDate expectedDeliveryDate,

    @Size(max = 5000, message = "Les notes ne doivent pas dépasser 5000 caractères.")
    String notes,

    @NotEmpty(message = "La commande doit contenir au moins un produit.")
    List<@Valid PurchaseOrderLineRequest> lines
) {}
