package com.ethan.printadmin.controller;

import com.ethan.printadmin.dto.CreatePrinterRequest;
import com.ethan.printadmin.model.Printer;
import com.ethan.printadmin.service.PrinterService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Printers")
@RestController
@RequestMapping("/printers")
public class PrinterController {
    private final PrinterService printerService;

    public PrinterController(PrinterService printerService) {
        this.printerService = printerService;
    }

    @Operation(summary = "List printers")
    @GetMapping
    public List<Printer> getPrinters() {
        return printerService.getPrinters();
    }

    @Operation(summary = "Create a printer")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Printer createPrinter(@Valid @RequestBody CreatePrinterRequest request) {
        return printerService.createPrinter(request);
    }
}
