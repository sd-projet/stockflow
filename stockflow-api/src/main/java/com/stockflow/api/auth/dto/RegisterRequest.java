package com.stockflow.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Le nom complet est obligatoire.")
        @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères.")
        @Pattern(regexp = ".*\\S.*", message = "Le nom complet ne peut pas contenir uniquement des espaces.")
        String fullName,

        @NotBlank(message = "L'adresse e-mail est obligatoire.")
        @Email(message = "L'adresse e-mail est invalide.")
        @Size(max = 255, message = "L'e-mail ne doit pas dépasser 255 caractères.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 12, max = 128, message = "Le mot de passe doit contenir entre 12 et 128 caractères.")
        String password
) {
}
