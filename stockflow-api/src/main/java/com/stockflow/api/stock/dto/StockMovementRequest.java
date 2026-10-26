
package com.stockflow.api.stock.dto;

import com.stockflow.api.stock.MovementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StockMovementRequest(

        @NotNull(message = "Le produit est obligatoire.")
        Long productId,

        @NotNull(message = "Le type de mouvement est obligatoire.")
        MovementType type,

        @NotNull(message = "La quantité est obligatoire.")
        @Positive(message = "La quantité doit être strictement positive.")
        Integer quantity,

        @NotBlank(message = "Le motif est obligatoire.")
        @Size(max = 500, message = "Le motif ne doit pas dépasser 500 caractères.")
        String reason,

        Long purchaseOrderLineId
) {
}
