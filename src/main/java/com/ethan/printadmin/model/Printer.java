package com.ethan.printadmin.model;

import jakarta.persistence.*;

@Entity
@Table(name = "printers")
public class Printer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String location;

    protected Printer() {
        // JPA uses this constructor when loading database rows.
    }

    public Printer(String name, String location) {
        this.name = name;
        this.location = location;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
}
