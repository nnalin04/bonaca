package com.bonaca.backend.members.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSharingGrantRequest(@NotNull Boolean visible) {
}
