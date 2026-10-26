
package com.stockflow.api.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record ProductRequest(

    @NotBlank(message = "La référence est obligatoire")
    @Size(max = 50, message = "La référence ne doit pas dépasser 50 caractères")
    String reference,

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
    String name,

    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
    String description,

    @NotNull(message = "La catégorie est obligatoire")
    @Positive(message = "L'identifiant de catégorie doit être positif")
    Long categoryId,

    @NotNull(message = "Le seuil de stock minimum est obligatoire")
    @PositiveOrZero(message = "Le seuil minimum ne peut pas être négatif")
    Integer minimumStock,

    @NotBlank(message = "L'unité est obligatoire")
    @Size(max = 30, message = "L'unité ne doit pas dépasser 30 caractères")
    String unit,
    
    @NotNull(message = "Le prix unitaire est obligatoire")
    @PositiveOrZero(message = "Le prix ne peut pas être négatif")
    BigDecimal unitPrice,
    
    @Valid
    @NotEmpty(message = "Un produit doit être associé à au moins un fournisseur")
    List<SupplierProductRequest> suppliers

) {}
