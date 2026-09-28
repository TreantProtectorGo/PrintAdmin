package com.ethan.printadmin.service;

import com.ethan.printadmin.dto.CreateUserRequest;
import com.ethan.printadmin.dto.UserUsageResponse;
import com.ethan.printadmin.exception.ResourceNotFoundException;
import com.ethan.printadmin.model.User;
import com.ethan.printadmin.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    private final MonthlyUsageService usage;

    public UserService(UserRepository userRepository, MonthlyUsageService usage) {
        this.userRepository = userRepository;
        this.usage = usage;
    }

    @Transactional(readOnly = true)
    public List<User> getUsers() {
        return userRepository.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public UserUsageResponse getUsage(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return usage.getUsage(user);
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        User user = new User(request.name().strip(), request.monthlyQuota());
        return userRepository.save(user);
    }
}
