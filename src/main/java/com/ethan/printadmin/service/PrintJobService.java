package com.ethan.printadmin.service;

import com.ethan.printadmin.dto.CreatePrintJobRequest;
import com.ethan.printadmin.dto.PrintJobResponse;
import com.ethan.printadmin.exception.QuotaExceededException;
import com.ethan.printadmin.exception.ResourceNotFoundException;
import com.ethan.printadmin.model.PrintJob;
import com.ethan.printadmin.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;

@Service
public class PrintJobService {
    private final UserRepository users;
    private final PrinterRepository printers;
    private final PrintJobRepository jobs;
    private final Clock clock;
    private final ZoneId quotaZone;

    public PrintJobService(UserRepository users, PrinterRepository printers, PrintJobRepository jobs,
                           Clock clock, @Value("${printadmin.quota-zone:Asia/Hong_Kong}") String quotaZone) {
        this.users = users;
        this.printers = printers;
        this.jobs = jobs;
        this.clock = clock;
        this.quotaZone = ZoneId.of(quotaZone);
    }

    @Transactional
    public PrintJobResponse createPrintJob(CreatePrintJobRequest request) {
        // Serialize submissions for this user so concurrent requests cannot spend the same allowance.
        var user = users.findForUpdate(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        var printer = printers.findById(request.printerId())
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found."));
        Instant now = clock.instant();
        LocalDate firstDay = now.atZone(quotaZone).toLocalDate().withDayOfMonth(1);
        Instant start = firstDay.atStartOfDay(quotaZone).toInstant();
        Instant end = firstDay.plusMonths(1).atStartOfDay(quotaZone).toInstant();
        long used = jobs.usedPages(user.getId(), start, end);
        if (used + request.pages() > user.getMonthlyQuota()) {
            throw new QuotaExceededException();
        }
        return PrintJobResponse.from(jobs.save(new PrintJob(user, printer, request.pages(), now)));
    }

    @Transactional(readOnly = true)
    public List<PrintJobResponse> getPrintJobs() {
        return jobs.findAll(Sort.by("id")).stream().map(PrintJobResponse::from).toList();
    }
}
