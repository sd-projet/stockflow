
package com.stockflow.api.user.service;

import com.stockflow.api.user.AppUser;
import com.stockflow.api.user.UserRole;
import com.stockflow.api.user.dto.UserCreateRequest;
import com.stockflow.api.user.dto.UserResponse;
import com.stockflow.api.user.repository.AppUserRepository;
import com.stockflow.api.common.exception.DuplicateResourceException;
import com.stockflow.api.common.exception.ResourceNotFoundException;
import com.stockflow.api.user.dto.UserUpdateRequest;
import com.stockflow.api.user.dto.UserPasswordUpdateRequest;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        String email = request.email().trim().toLowerCase();

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException(
                    "Un utilisateur avec cette adresse e-mail existe déjà.");
        }

        AppUser user = new AppUser();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role() != null
                ? request.role()
                : UserRole.STOCK_MANAGER);
        user.setEnabled(true);

        AppUser savedUser = appUserRepository.save(user);

        return toResponse(savedUser);
    }

    public List<UserResponse> getAllUsers() {
        return appUserRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec l'identifiant : " + id));

        return toResponse(user);
    }

    @Transactional
    public UserResponse disableUser(Long id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec l'identifiant : " + id));

        user.setEnabled(false);

        AppUser savedUser = appUserRepository.save(user);

        return toResponse(savedUser);
    }

    @Transactional
    public UserResponse enableUser(Long id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec l'identifiant : " + id));

        user.setEnabled(true);

        AppUser savedUser = appUserRepository.save(user);

        return toResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUserPassword(
            Long id,
            UserPasswordUpdateRequest request) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec l'identifiant : " + id));

        user.setPasswordHash(passwordEncoder.encode(request.password()));

        AppUser savedUser = appUserRepository.save(user);

        return toResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec l'identifiant : " + id));

        String email = request.email().trim().toLowerCase();

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(email);

        if (emailChanged && appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException(
                    "Un utilisateur avec cette adresse e-mail existe déjà.");
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setRole(request.role());

        AppUser savedUser = appUserRepository.save(user);

        return toResponse(savedUser);
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getEnabled(),
                user.getCreatedAt());
    }
}
