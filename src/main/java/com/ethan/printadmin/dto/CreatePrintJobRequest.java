package com.ethan.printadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePrintJobRequest(
        @NotNull @Positive @Schema(example = "1") Long userId,
        @NotNull @Positive @Schema(example = "1") Long printerId,
        @NotNull @Positive @Schema(example = "20") Integer pages) {
}
