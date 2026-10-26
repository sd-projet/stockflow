
package com.stockflow.api.security;

import com.stockflow.api.user.AppUser;
import com.stockflow.api.user.repository.AppUserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

        private final JwtService jwtService;
        private final AppUserRepository appUserRepository;

        private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

        public JwtAuthenticationFilter(
                        JwtService jwtService,
                        AppUserRepository appUserRepository) {
                this.jwtService = jwtService;
                this.appUserRepository = appUserRepository;
        }

        @Override
        protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain) throws ServletException, IOException {

                String header = request.getHeader("Authorization");

                if (header == null || !header.startsWith("Bearer ")) {
                        filterChain.doFilter(request, response);
                        return;
                }

                String token = header.substring(7);

                try {
                        String email = jwtService.extractEmail(token);

                        if (SecurityContextHolder.getContext().getAuthentication() == null) {
                                AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                                                .orElse(null);

                                if (user != null
                                                && jwtService.isTokenValid(token, user)) {

                                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                                        user,
                                                        null,
                                                        List.of(new SimpleGrantedAuthority(
                                                                        "ROLE_" + user.getRole().name())));

                                        SecurityContextHolder.getContext()
                                                        .setAuthentication(authentication);
                                }
                        }
                } catch (JwtException | IllegalArgumentException exception) {
                        logger.warn("Échec de validation JWT : {} - {}",
                                        exception.getClass().getSimpleName(),
                                        exception.getMessage());
                        SecurityContextHolder.clearContext();
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                        "{\"message\":\"Jeton JWT invalide ou expiré.\"}");
                        return;
                }

                filterChain.doFilter(request, response);
        }
}
