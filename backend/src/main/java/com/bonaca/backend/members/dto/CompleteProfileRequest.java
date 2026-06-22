package com.bonaca.backend.members.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CompleteProfileRequest(
        @NotBlank String name, String gender, LocalDate dob, Integer heightCm, Integer weightKg) {
}
