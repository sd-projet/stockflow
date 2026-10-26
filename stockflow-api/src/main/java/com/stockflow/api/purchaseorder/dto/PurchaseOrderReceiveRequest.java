
package com.stockflow.api.purchaseorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PurchaseOrderReceiveRequest(
        @NotEmpty(message = "La liste des produits reçus est obligatoire.")
        @Size(max = 100, message = "La réception ne peut pas contenir plus de 100 lignes.")
        List<@Valid ReceivedLine> lines
) {
    public record ReceivedLine(
            @NotNull(message = "L'identifiant de la ligne de commande est obligatoire.")
            @Positive(message = "L'identifiant de la ligne doit être positif.")
            Long lineId,

            @NotNull(message = "La quantité reçue est obligatoire.")
            @Positive(message = "La quantité reçue doit être strictement positive.")
            Integer quantityReceived
    ) {
    }
}
