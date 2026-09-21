package com.ethan.printadmin.dto;

import com.ethan.printadmin.model.PrintJob;
import java.time.Instant;

public record PrintJobResponse(Long id, Long userId, Long printerId, int pages, Instant createdAt) {
    public static PrintJobResponse from(PrintJob job) {
        return new PrintJobResponse(job.getId(), job.getUser().getId(),
                job.getPrinter().getId(), job.getPages(), job.getCreatedAt());
    }
}
