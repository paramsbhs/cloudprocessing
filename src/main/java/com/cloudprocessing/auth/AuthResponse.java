package com.cloudprocessing.auth;

import java.util.UUID;

public record AuthResponse(
    String token,
    UUID userId,
    String email
) {}
