package com.ethan.printadmin.controller;

import com.ethan.printadmin.dto.CreatePrinterRequest;
import com.ethan.printadmin.model.Printer;
import com.ethan.printadmin.service.PrinterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/printers")
public class PrinterController {
    private final PrinterService printerService;

    public PrinterController(PrinterService printerService) {
        this.printerService = printerService;
    }

    @GetMapping
    public List<Printer> getPrinters() {
        return printerService.getPrinters();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Printer createPrinter(@Valid @RequestBody CreatePrinterRequest request) {
        return printerService.createPrinter(request);
    }
}
