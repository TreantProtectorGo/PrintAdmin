package com.ethan.printadmin.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "print_jobs", indexes = @Index(name = "idx_jobs_user_created", columnList = "user_id,created_at"))
public class PrintJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "printer_id", nullable = false)
    private Printer printer;
    @Column(nullable = false)
    private int pages;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PrintJob() {}

    public PrintJob(User user, Printer printer, int pages, Instant createdAt) {
        this.user = user;
        this.printer = printer;
        this.pages = pages;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Printer getPrinter() { return printer; }
    public int getPages() { return pages; }
    public Instant getCreatedAt() { return createdAt; }
}
