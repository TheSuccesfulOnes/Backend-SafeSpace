package com.experimentos.backend.shared.security;

import com.experimentos.backend.iam.infrastructure.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String username = jwtService.username(header.substring(7));
                users.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
                        .ifPresent(
                                user -> {
                                    if (user.isEnabled()) {
                                        var authentication =
                                                new UsernamePasswordAuthenticationToken(
                                                        user.getUsername(),
                                                        null,
                                                        List.of(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_"
                                                                                + user.getRole()
                                                                                        .name())));
                                        SecurityContextHolder.getContext()
                                                .setAuthentication(authentication);
                                    }
                                });
            } catch (RuntimeException exception) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
