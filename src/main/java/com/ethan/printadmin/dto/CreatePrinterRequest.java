package com.ethan.printadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePrinterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 200) String location) {
}
