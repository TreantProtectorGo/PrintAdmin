package com.ethan.printadmin.service;

import com.ethan.printadmin.dto.CreatePrinterRequest;
import com.ethan.printadmin.model.Printer;
import com.ethan.printadmin.repository.PrinterRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PrinterService {
    private final PrinterRepository printerRepository;

    public PrinterService(PrinterRepository printerRepository) {
        this.printerRepository = printerRepository;
    }

    @Transactional(readOnly = true)
    public List<Printer> getPrinters() {
        return printerRepository.findAll(Sort.by("id"));
    }

    @Transactional
    public Printer createPrinter(CreatePrinterRequest request) {
        Printer printer = new Printer(request.name().strip(), request.location().strip());
        return printerRepository.save(printer);
    }
}
