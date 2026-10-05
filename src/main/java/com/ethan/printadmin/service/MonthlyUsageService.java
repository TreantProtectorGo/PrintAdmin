package com.ethan.printadmin.service;

import com.ethan.printadmin.dto.UserUsageResponse;
import com.ethan.printadmin.model.User;
import com.ethan.printadmin.repository.PrintJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.*;

@Service
public class MonthlyUsageService {
    private final PrintJobRepository jobs;
    private final Clock clock;
    private final ZoneId quotaZone;

    public MonthlyUsageService(PrintJobRepository jobs, Clock clock,
                               @Value("${printadmin.quota-zone:Asia/Hong_Kong}") String quotaZone) {
        this.jobs = jobs;
        this.clock = clock;
        this.quotaZone = ZoneId.of(quotaZone);
    }

    public long usedPages(Long userId, Instant now) {
        MonthRange range = monthRange(YearMonth.from(now.atZone(quotaZone)));
        return jobs.usedPages(userId, range.start(), range.end());
    }

    public MonthRange monthRange(YearMonth month) {
        Instant start = month.atDay(1).atStartOfDay(quotaZone).toInstant();
        Instant end = month.plusMonths(1).atDay(1).atStartOfDay(quotaZone).toInstant();
        return new MonthRange(start, end);
    }

    public record MonthRange(Instant start, Instant end) {}

    public UserUsageResponse getUsage(User user) {
        long used = usedPages(user.getId(), clock.instant());
        return new UserUsageResponse(user.getId(), user.getMonthlyQuota(), used,
                Math.max(0L, user.getMonthlyQuota() - used));
    }
}
