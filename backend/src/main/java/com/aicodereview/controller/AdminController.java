package com.aicodereview.controller;

import com.aicodereview.dto.UpdateUserRequest;
import com.aicodereview.dto.UserResponse;
import com.aicodereview.entity.User;
import com.aicodereview.exception.ResourceNotFoundException;
import com.aicodereview.repository.RefreshTokenRepository;
import com.aicodereview.repository.UserRepository;
import com.aicodereview.security.AuthenticatedUser;
import com.aicodereview.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Administrative endpoints - restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;

    public AdminController(UserRepository users, RefreshTokenRepository refreshTokens) {
        this.users = users;
        this.refreshTokens = refreshTokens;
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return users.findAll().stream().map(AuthService::toResponse).toList();
    }

    @PutMapping("/users/{id}")
    @Transactional
    public UserResponse updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser current) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (user.getId().equals(current.id()) && (Boolean.FALSE.equals(request.getEnabled()) ||
                (request.getRole() != null && request.getRole() != user.getRole()))) {
            throw new IllegalArgumentException("You cannot disable or change the role of your own account");
        }
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
            if (!request.getEnabled()) refreshTokens.revokeAllForUser(user.getId());
        }
        return AuthService.toResponse(users.save(user));
    }
}
