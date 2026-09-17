package com.ethan.printadmin.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int monthlyQuota;

    protected User() {
        // JPA uses this constructor when loading database rows.
    }

    public User(String name, int monthlyQuota) {
        this.name = name;
        this.monthlyQuota = monthlyQuota;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getMonthlyQuota() { return monthlyQuota; }
}
