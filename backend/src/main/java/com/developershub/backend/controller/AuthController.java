package com.developershub.backend.controller;

import com.developershub.backend.dto.AuthRequest;
import com.developershub.backend.dto.AuthResponse;
import com.developershub.backend.dto.BootstrapRequest;
import com.developershub.backend.dto.RefreshRequest;
import com.developershub.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Register, log in, refresh tokens, and log out")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register", description = "Creates an account and returns an access token and a refresh token")
    public AuthResponse register(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, httpRequest);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Returns a fresh access token and refresh token")
    public AuthResponse login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Exchanges a refresh token for a new pair; the old token is retired")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Log out", description = "Revokes a refresh token")
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
    }

    /**
     * Grants the ADMIN role to an existing account when no admin exists yet.
     * Requires the ADMIN_SETUP_TOKEN configured on the server.
     */
    @PostMapping("/admin/bootstrap")
    @Operation(summary = "Bootstrap the first admin", description = "Grants ADMIN to an existing account while no admin exists; needs ADMIN_SETUP_TOKEN")
    public AuthResponse bootstrapAdmin(@Valid @RequestBody BootstrapRequest request, HttpServletRequest httpRequest) {
        return authService.bootstrapAdmin(request, httpRequest);
    }
}
