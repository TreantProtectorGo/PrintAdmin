package com.ethan.printadmin;

import com.ethan.printadmin.dto.CreatePrintJobRequest;
import com.ethan.printadmin.exception.QuotaExceededException;
import com.ethan.printadmin.model.*;
import com.ethan.printadmin.repository.*;
import com.ethan.printadmin.service.PrintJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PrintJobApiTests.FixedTime.class, PostgresTestConfiguration.class})
class PrintJobApiTests {
    private static final Instant NOW = Instant.parse("2026-09-15T04:00:00Z");
    @TestConfiguration
    static class FixedTime {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
    }
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PrinterRepository printers;
    @Autowired PrintJobRepository jobs;
    @Autowired PrintJobService service;
    User user;
    Printer printer;

    @BeforeEach
    void setUp() {
        jobs.deleteAll();
        users.deleteAll();
        printers.deleteAll();
        user = users.save(new User("Ethan", 100));
        printer = printers.save(new Printer("Staff Printer", "2/F"));
    }

    String request(Long userId, Long printerId, int pages) {
        return "{\"userId\":" + userId + ",\"printerId\":" + printerId + ",\"pages\":" + pages + "}";
    }

    @Test
    void savesListsAndRejectsJobsOverQuota() throws Exception {
        mvc.perform(get("/print-jobs")).andExpect(content().json("[]"));
        for (int pages : new int[]{80, 20}) {
            mvc.perform(post("/print-jobs").contentType("application/json")
                    .content(request(user.getId(), printer.getId(), pages)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.userId").value(user.getId()))
                    .andExpect(jsonPath("$.printerId").value(printer.getId()))
                    .andExpect(jsonPath("$.createdAt").value(NOW.toString()));
        }
        mvc.perform(post("/print-jobs").contentType("application/json")
                .content(request(user.getId(), printer.getId(), 1)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").exists());
        assertThat(jobs.count()).isEqualTo(2);
        mvc.perform(get("/print-jobs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].pages").value(80))
                .andExpect(jsonPath("$[1].pages").value(20));
    }

    @Test
    void countsOnlyThisUserAndCurrentCalendarMonth() {
        Instant start = Instant.parse("2026-08-31T16:00:00Z");
        Instant end = Instant.parse("2026-09-30T16:00:00Z");
        jobs.save(new PrintJob(user, printer, 100, start.minusSeconds(1)));
        jobs.save(new PrintJob(user, printer, 30, start));
        jobs.save(new PrintJob(user, printer, 20, end.minusSeconds(1)));
        jobs.save(new PrintJob(user, printer, 100, end));
        User other = users.save(new User("Alex", 100));
        jobs.save(new PrintJob(other, printer, 100, NOW));
        assertThat(jobs.usedPages(user.getId(), start, end)).isEqualTo(50);
        assertThat(service.createPrintJob(new CreatePrintJobRequest(user.getId(), printer.getId(), 50)).pages())
                .isEqualTo(50);
    }

    @Test
    void missingReferencesReturn404WithoutSaving() throws Exception {
        for (String body : new String[]{request(Long.MAX_VALUE, printer.getId(), 1),
                request(user.getId(), Long.MAX_VALUE, 1)}) {
            mvc.perform(post("/print-jobs").contentType("application/json").content(body))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").exists());
        }
        assertThat(jobs.count()).isZero();
    }

    @Test
    void invalidInputsReturn400WithoutSaving() throws Exception {
        for (String body : new String[]{"{}", "{\"userId\":null,\"printerId\":null,\"pages\":null}",
                request(user.getId(), printer.getId(), 0), request(user.getId(), printer.getId(), -1),
                request(0L, printer.getId(), 1), request(user.getId(), -1L, 1), "{broken"}) {
            mvc.perform(post("/print-jobs").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        assertThat(jobs.count()).isZero();
    }

    @Test
    void zeroQuotaAndHugePageCountsAreRejected() throws Exception {
        User zero = users.save(new User("Zero", 0));
        for (String body : new String[]{request(zero.getId(), printer.getId(), 1),
                request(user.getId(), printer.getId(), Integer.MAX_VALUE)}) {
            mvc.perform(post("/print-jobs").contentType("application/json").content(body))
                    .andExpect(status().isConflict());
        }
        assertThat(jobs.count()).isZero();
    }

    @Test
    void usageStartsAtZeroAndIncludesAcceptedJobsOnly() throws Exception {
        mvc.perform(get("/users/{id}/usage", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.monthlyQuota").value(100))
                .andExpect(jsonPath("$.usedPages").value(0))
                .andExpect(jsonPath("$.remainingPages").value(100));
        service.createPrintJob(new CreatePrintJobRequest(user.getId(), printer.getId(), 80));
        mvc.perform(post("/print-jobs").contentType("application/json")
                .content(request(user.getId(), printer.getId(), 21)))
                .andExpect(status().isConflict());
        mvc.perform(get("/users/{id}/usage", user.getId()))
                .andExpect(jsonPath("$.usedPages").value(80))
                .andExpect(jsonPath("$.remainingPages").value(20));
        service.createPrintJob(new CreatePrintJobRequest(user.getId(), printer.getId(), 20));
        mvc.perform(get("/users/{id}/usage", user.getId()))
                .andExpect(jsonPath("$.usedPages").value(100))
                .andExpect(jsonPath("$.remainingPages").value(0));
        assertThat(jobs.count()).isEqualTo(2);
    }

    @Test
    void usageFiltersByUserAndMonthAcrossAllPrinters() throws Exception {
        Instant start = Instant.parse("2026-08-31T16:00:00Z");
        Instant end = Instant.parse("2026-09-30T16:00:00Z");
        Printer second = printers.save(new Printer("Library", "1/F"));
        User other = users.save(new User("Alex", 100));
        jobs.save(new PrintJob(user, printer, 100, start.minusSeconds(1)));
        jobs.save(new PrintJob(user, printer, 30, start));
        jobs.save(new PrintJob(user, second, 20, end.minusSeconds(1)));
        jobs.save(new PrintJob(user, printer, 100, end));
        jobs.save(new PrintJob(other, printer, 100, NOW));
        mvc.perform(get("/users/{id}/usage", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedPages").value(50))
                .andExpect(jsonPath("$.remainingPages").value(50));
    }

    @Test
    void usageHandlesZeroQuotaAndMissingUser() throws Exception {
        User zero = users.save(new User("Zero", 0));
        mvc.perform(get("/users/{id}/usage", zero.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedPages").value(0))
                .andExpect(jsonPath("$.remainingPages").value(0));
        mvc.perform(get("/users/{id}/usage", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("User not found."));
        mvc.perform(get("/users/not-a-number/usage")).andExpect(status().isBadRequest());
    }

    @Test
    void usageNeverReportsNegativeRemainingPages() throws Exception {
        // Simulate historical data imported outside the quota-checked API.
        jobs.save(new PrintJob(user, printer, 120, NOW));
        mvc.perform(get("/users/{id}/usage", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedPages").value(120))
                .andExpect(jsonPath("$.remainingPages").value(0));
    }

    @Test
    void simultaneousSubmissionsCannotExceedQuota() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> submit = () -> {
                start.await();
                try {
                    service.createPrintJob(new CreatePrintJobRequest(user.getId(), printer.getId(), 60));
                    return true;
                } catch (QuotaExceededException exception) {
                    return false;
                }
            };
            Future<Boolean> first = executor.submit(submit);
            Future<Boolean> second = executor.submit(submit);
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS) ^ second.get(10, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(jobs.count()).isEqualTo(1);
    }
}
