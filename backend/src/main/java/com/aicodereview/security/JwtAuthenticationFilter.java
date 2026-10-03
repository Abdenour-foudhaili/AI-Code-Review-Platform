package com.aicodereview.security;

import com.aicodereview.entity.User;
import com.aicodereview.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Reads "Authorization: Bearer <jwt>", validates it, and populates the SecurityContext (stateless). */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    static final String ERROR_ATTRIBUTE = "jwt_error";

    private final JwtService jwtService;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7).trim();
            try {
                AuthenticatedUser parsed = jwtService.parseAccessToken(token);
                User user = users.findById(parsed.id()).orElse(null);
                if (user == null || !user.isEnabled()) {
                    request.setAttribute(ERROR_ATTRIBUTE, "Account disabled or not found");
                } else {
                    // role is re-read from the database so a demoted user loses privileges immediately
                    AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole().name());
                    var auth = new UsernamePasswordAuthenticationToken(principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                String msg = e.getMessage() != null && e.getMessage().toLowerCase().contains("expired")
                        ? "JWT expired" : "Invalid JWT";
                request.setAttribute(ERROR_ATTRIBUTE, msg);
            }
        }
        chain.doFilter(request, response);
    }
}
