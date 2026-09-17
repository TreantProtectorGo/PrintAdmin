package com.ethan.printadmin.service;

import com.ethan.printadmin.dto.CreateUserRequest;
import com.ethan.printadmin.model.User;
import com.ethan.printadmin.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<User> getUsers() {
        return userRepository.findAll(Sort.by("id"));
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        User user = new User(request.name().strip(), request.monthlyQuota());
        return userRepository.save(user);
    }
}
