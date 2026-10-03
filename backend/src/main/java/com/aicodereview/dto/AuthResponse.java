package com.aicodereview.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {}
