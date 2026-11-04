package com.developershub.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access token, with a refresh token when available")
public class AuthResponse {

    @Schema(description = "JWT access token")
    private String token;
    @Schema(description = "Opaque refresh token, single use")
    private String refreshToken;

    public AuthResponse(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
