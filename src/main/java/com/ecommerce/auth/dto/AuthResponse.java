package com.ecommerce.auth.dto;

import com.ecommerce.user.model.Role;

public record AuthResponse(String token, String tokenType, long expiresInMs, UserSummary user) {
    public record UserSummary(Long id, String fullName, String email, Role role) {}
}
