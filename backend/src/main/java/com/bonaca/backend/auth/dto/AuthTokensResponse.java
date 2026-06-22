package com.bonaca.backend.auth.dto;

public record AuthTokensResponse(String accessToken, String refreshToken, boolean profileCompleted) {
}
