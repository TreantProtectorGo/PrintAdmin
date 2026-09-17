package com.ethan.printadmin.repository;

import com.ethan.printadmin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
