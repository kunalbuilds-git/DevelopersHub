package com.developershub.backend.controller;

import com.developershub.backend.dto.AuthRequest;
import com.developershub.backend.dto.AuthResponse;
import com.developershub.backend.dto.BootstrapRequest;
import com.developershub.backend.dto.RefreshRequest;
import com.developershub.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, httpRequest);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
    }

    /**
     * Grants the ADMIN role to an existing account when no admin exists yet.
     * Requires the ADMIN_SETUP_TOKEN configured on the server.
     */
    @PostMapping("/admin/bootstrap")
    public AuthResponse bootstrapAdmin(@Valid @RequestBody BootstrapRequest request, HttpServletRequest httpRequest) {
        return authService.bootstrapAdmin(request, httpRequest);
    }
}
