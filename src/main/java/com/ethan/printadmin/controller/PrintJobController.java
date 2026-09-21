package com.ethan.printadmin.controller;

import com.ethan.printadmin.dto.CreatePrintJobRequest;
import com.ethan.printadmin.dto.PrintJobResponse;
import com.ethan.printadmin.service.PrintJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/print-jobs")
public class PrintJobController {
    private final PrintJobService service;

    public PrintJobController(PrintJobService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrintJobResponse createPrintJob(@Valid @RequestBody CreatePrintJobRequest request) {
        return service.createPrintJob(request);
    }

    @GetMapping
    public List<PrintJobResponse> getPrintJobs() { return service.getPrintJobs(); }
}
