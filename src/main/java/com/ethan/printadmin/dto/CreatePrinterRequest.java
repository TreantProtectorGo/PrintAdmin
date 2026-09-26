package com.ethan.printadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePrinterRequest(
        @NotBlank @Size(max = 100) @Schema(example = "Staff Room Printer") String name,
        @NotBlank @Size(max = 200) @Schema(example = "2/F staff room") String location) {
}
