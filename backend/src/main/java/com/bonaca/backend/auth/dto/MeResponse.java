package com.bonaca.backend.auth.dto;

import java.util.UUID;

public record MeResponse(UUID userId, String phoneNumber, boolean profileCompleted) {
}
