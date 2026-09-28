package com.ethan.printadmin.controller;

import com.ethan.printadmin.dto.CreateUserRequest;
import com.ethan.printadmin.dto.UserUsageResponse;
import com.ethan.printadmin.model.User;
import com.ethan.printadmin.service.UserService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users")
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List users")
    @GetMapping
    public List<User> getUsers() {
        return userService.getUsers();
    }

    @Operation(summary = "Get current monthly usage",
            description = "Pages used across all printers in the configured calendar month. Remaining pages never fall below zero.")
    @ApiResponse(responseCode = "200", description = "Current usage")
    @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}/usage")
    public UserUsageResponse getUsage(@PathVariable Long id) {
        return userService.getUsage(id);
    }

    @Operation(summary = "Create a user")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }
}
