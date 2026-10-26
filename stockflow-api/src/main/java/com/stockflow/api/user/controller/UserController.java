
package com.stockflow.api.user.controller;

import com.stockflow.api.user.AppUser;
import com.stockflow.api.user.dto.UserCreateRequest;
import com.stockflow.api.user.dto.UserResponse;
import com.stockflow.api.user.service.UserService;
import com.stockflow.api.user.dto.UserUpdateRequest;
import com.stockflow.api.user.dto.UserPasswordUpdateRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PatchMapping("/{id}/disable")
    public UserResponse disableUser(
            @PathVariable Long id,
            Authentication authentication) {
        if (authentication != null
                && authentication.getPrincipal() instanceof AppUser currentUser
                && id.equals(currentUser.getId())) {
            throw new IllegalStateException(
                    "Vous ne pouvez pas désactiver votre propre compte.");
        }

        return userService.disableUser(id);
    }

    @PatchMapping("/{id}/enable")
    public UserResponse enableUser(@PathVariable Long id) {
        return userService.enableUser(id);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateUser(id, request);
    }

    @PatchMapping("/{id}/password")
    public UserResponse updateUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody UserPasswordUpdateRequest request) {
        return userService.updateUserPassword(id, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @Valid @RequestBody UserCreateRequest request) {
        return userService.createUser(request);
    }
}
