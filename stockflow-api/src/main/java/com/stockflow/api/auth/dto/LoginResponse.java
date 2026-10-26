
package com.stockflow.api.auth.dto;

import com.stockflow.api.user.UserRole;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        Long userId,
        String fullName,
        String email,
        UserRole role
) {
}
