package com.aicodereview.security;

/** Principal stored in the SecurityContext after JWT validation. */
public record AuthenticatedUser(Long id, String email, String role) {
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
