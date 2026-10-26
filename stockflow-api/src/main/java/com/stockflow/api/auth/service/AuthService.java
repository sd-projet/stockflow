
package com.stockflow.api.auth.service;

import com.stockflow.api.auth.dto.LoginRequest;
import com.stockflow.api.auth.dto.LoginResponse;
import com.stockflow.api.auth.dto.RegisterRequest;
import com.stockflow.api.common.exception.DuplicateResourceException;
import com.stockflow.api.user.UserRole;
import com.stockflow.api.security.JwtService;
import com.stockflow.api.user.AppUser;
import com.stockflow.api.user.repository.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }


    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException(
                    "Un compte avec cette adresse e-mail existe déjà.");
        }

        AppUser user = new AppUser();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        // Le rôle est imposé côté serveur : le client ne peut pas s'inscrire administrateur.
        user.setRole(UserRole.STOCK_MANAGER);
        user.setEnabled(true);

        AppUser savedUser = appUserRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return new LoginResponse(
                token,
                "Bearer",
                86400,
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository
                .findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException(
                        "Adresse e-mail ou mot de passe incorrect."
                ));

        if (!Boolean.TRUE.equals(user.getEnabled())
                || !passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                )) {
            throw new BadCredentialsException(
                    "Adresse e-mail ou mot de passe incorrect."
            );
        }

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                86400,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
