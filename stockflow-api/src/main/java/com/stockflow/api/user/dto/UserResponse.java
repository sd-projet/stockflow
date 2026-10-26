
package com.stockflow.api.user.dto;

import com.stockflow.api.user.UserRole;
import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        UserRole role,
        Boolean enabled,
        OffsetDateTime createdAt
) {
}
