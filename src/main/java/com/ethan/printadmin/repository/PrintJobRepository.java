package com.ethan.printadmin.repository;

import com.ethan.printadmin.model.PrintJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface PrintJobRepository extends JpaRepository<PrintJob, Long> {
    @Query("select coalesce(sum(j.pages), 0) from PrintJob j where j.user.id = :userId "
            + "and j.createdAt >= :start and j.createdAt < :end")
    long usedPages(@Param("userId") Long userId, @Param("start") Instant start, @Param("end") Instant end);
}
