package com.stockflow.api.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(

        @NotBlank(message = "Le nom du fournisseur est obligatoire.")
        @Size(max = 150, message = "Le nom ne peut pas dépasser 150 caractères.")
        String name,

        @Size(max = 100, message = "Le nom du contact ne peut pas dépasser 100 caractères.")
        String contactName,

        @Email(message = "L'adresse e-mail est invalide.")
        @Size(max = 255, message = "L'e-mail ne peut pas dépasser 255 caractères.")
        String email,

        @Size(max = 30, message = "Le téléphone ne peut pas dépasser 30 caractères.")
        String phone,

        @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères.")
        String address,

        @Size(max = 100, message = "La ville ne peut pas dépasser 100 caractères.")
        String city,

        @Size(max = 20, message = "Le code postal ne peut pas dépasser 20 caractères.")
        String postalCode,

        @Size(max = 100, message = "Le pays ne peut pas dépasser 100 caractères.")
        String country
) {
}
