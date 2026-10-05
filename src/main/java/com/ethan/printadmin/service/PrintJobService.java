package com.ethan.printadmin.service;

import com.ethan.printadmin.dto.CreatePrintJobRequest;
import com.ethan.printadmin.dto.PrintJobResponse;
import com.ethan.printadmin.exception.QuotaExceededException;
import com.ethan.printadmin.exception.InvalidFilterException;
import com.ethan.printadmin.exception.ResourceNotFoundException;
import com.ethan.printadmin.model.PrintJob;
import com.ethan.printadmin.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class PrintJobService {
    private final UserRepository users;
    private final PrinterRepository printers;
    private final PrintJobRepository jobs;
    private final Clock clock;
    private final MonthlyUsageService usage;

    public PrintJobService(UserRepository users, PrinterRepository printers, PrintJobRepository jobs,
                           Clock clock, MonthlyUsageService usage) {
        this.users = users;
        this.printers = printers;
        this.jobs = jobs;
        this.clock = clock;
        this.usage = usage;
    }

    @Transactional
    public PrintJobResponse createPrintJob(CreatePrintJobRequest request) {
        // Serialize submissions for this user so concurrent requests cannot spend the same allowance.
        var user = users.findForUpdate(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        var printer = printers.findById(request.printerId())
                .orElseThrow(() -> new ResourceNotFoundException("Printer not found."));
        Instant now = clock.instant();
        long used = usage.usedPages(user.getId(), now);
        if (used + request.pages() > user.getMonthlyQuota()) {
            throw new QuotaExceededException();
        }
        return PrintJobResponse.from(jobs.save(new PrintJob(user, printer, request.pages(), now)));
    }

    @Transactional(readOnly = true)
    public List<PrintJobResponse> getPrintJobs(Long userId, Long printerId, String month) {
        if ((userId != null && userId <= 0) || (printerId != null && printerId <= 0)) {
            throw new InvalidFilterException("User and printer IDs must be positive.");
        }
        MonthlyUsageService.MonthRange range = null;
        if (month != null) {
            if (!month.matches("[0-9]{4}-(0[1-9]|1[0-2])") || month.startsWith("0000")) {
                throw new InvalidFilterException("Month must use YYYY-MM, for example 2026-09.");
            }
            range = usage.monthRange(YearMonth.parse(month));
        }
        var monthRange = range;
        Specification<PrintJob> filter = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) predicates.add(builder.equal(root.get("user").get("id"), userId));
            if (printerId != null) predicates.add(builder.equal(root.get("printer").get("id"), printerId));
            if (monthRange != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), monthRange.start()));
                predicates.add(builder.lessThan(root.get("createdAt"), monthRange.end()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return jobs.findAll(filter, Sort.by("id")).stream().map(PrintJobResponse::from).toList();
    }
}
