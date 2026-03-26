package com.aque.auth.dto.response;

public record LoginResponse(
        String token,
        long expiresIn
) {
}