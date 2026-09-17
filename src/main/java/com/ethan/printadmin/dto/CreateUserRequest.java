package com.ethan.printadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// An Integer lets validation distinguish a missing quota from a valid zero.
public record CreateUserRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @PositiveOrZero Integer monthlyQuota) {
}
