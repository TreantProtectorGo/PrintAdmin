package com.ethan.printadmin.controller;

import com.ethan.printadmin.dto.CreatePrintJobRequest;
import com.ethan.printadmin.dto.PrintJobResponse;
import com.ethan.printadmin.service.PrintJobService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Print jobs")
@RestController
@RequestMapping("/print-jobs")
public class PrintJobController {
    private final PrintJobService service;

    public PrintJobController(PrintJobService service) { this.service = service; }

    @Operation(summary = "Record a print job")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "User or printer not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Monthly quota exceeded; no job is saved",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrintJobResponse createPrintJob(@Valid @RequestBody CreatePrintJobRequest request) {
        return service.createPrintJob(request);
    }

    @Operation(summary = "List print jobs", description = "Optional filters combine with AND. Results are ordered by ID.")
    @ApiResponse(responseCode = "400", description = "Invalid filter",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public List<PrintJobResponse> getPrintJobs(
            @Parameter(description = "Positive user ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "Positive printer ID") @RequestParam(required = false) Long printerId,
            @Parameter(description = "Calendar month in the quota timezone (default Asia/Hong_Kong)", example = "2026-09")
            @RequestParam(required = false) String month) {
        return service.getPrintJobs(userId, printerId, month);
    }
}
