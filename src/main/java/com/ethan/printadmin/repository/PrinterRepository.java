package com.ethan.printadmin.repository;

import com.ethan.printadmin.model.Printer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrinterRepository extends JpaRepository<Printer, Long> {
}
