
package com.stockflow.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordUpdateRequest(
    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(
        min = 12,
        max = 128,
        message = "Le mot de passe doit contenir entre 12 et 128 caractères."
    )
    String password
) {}
