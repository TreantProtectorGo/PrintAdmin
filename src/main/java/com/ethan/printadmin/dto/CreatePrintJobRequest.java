package com.ethan.printadmin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePrintJobRequest(
        @NotNull @Positive Long userId,
        @NotNull @Positive Long printerId,
        @NotNull @Positive Integer pages) {
}
