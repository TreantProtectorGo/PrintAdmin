package com.ethan.printadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// An Integer lets validation distinguish a missing quota from a valid zero.
public record CreateUserRequest(
        @NotBlank @Size(max = 100) @Schema(example = "Ethan") String name,
        @NotNull @PositiveOrZero @Schema(example = "100") Integer monthlyQuota) {
}
