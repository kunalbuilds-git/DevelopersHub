package com.developershub.backend.service;

import com.developershub.backend.dto.AuthRequest;
import com.developershub.backend.dto.AuthResponse;
import com.developershub.backend.dto.BootstrapRequest;
import com.developershub.backend.dto.RefreshRequest;
import com.developershub.backend.entity.RefreshToken;
import com.developershub.backend.entity.User;
import com.developershub.backend.repository.RefreshTokenRepository;
import com.developershub.backend.repository.UserRepository;
import com.developershub.backend.security.JwtUtil;
import com.developershub.backend.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    // Hash of an unusable random password; used to equalize the BCrypt cost of
    // "unknown email" and "wrong password" responses so timing cannot enumerate accounts.
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RateLimiter rateLimiter;

    private final long refreshTokenTtlDays;
    private final String adminSetupToken;
    private final int loginMax;
    private final int registerMax;
    private final int bootstrapMax;
    private final Duration window;

    @Autowired
    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            RateLimiter rateLimiter,
            @Value("${app.refresh-token.expiration-days:7}") long refreshTokenTtlDays,
            @Value("${app.admin.setup-token:}") String adminSetupToken,
            @Value("${app.rate-limit.login.max:10}") int loginMax,
            @Value("${app.rate-limit.register.max:5}") int registerMax,
            @Value("${app.rate-limit.bootstrap.max:3}") int bootstrapMax,
            @Value("${app.rate-limit.window-seconds:60}") int windowSeconds) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.rateLimiter = rateLimiter;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
        this.adminSetupToken = adminSetupToken == null ? "" : adminSetupToken.trim();
        this.loginMax = loginMax;
        this.registerMax = registerMax;
        this.bootstrapMax = bootstrapMax;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    @Transactional
    public AuthResponse register(AuthRequest request, HttpServletRequest httpRequest) {
        requireCapacity("register", clientIp(httpRequest), registerMax);

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(AuthRequest request, HttpServletRequest httpRequest) {
        requireCapacity("login", clientIp(httpRequest), loginMax);

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            // Burn the same BCrypt cost as a real check so response timing cannot reveal
            // whether the email exists.
            passwordEncoder.matches(request.getPassword(), DUMMY_PASSWORD_HASH);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(request.getRefreshToken()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (!stored.isUsable(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        // Rotation: a refresh token is single-use. A replayed one finds the row revoked.
        stored.revoke();
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByTokenHash(hash(request.getRefreshToken()))
                .ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public AuthResponse bootstrapAdmin(BootstrapRequest request, HttpServletRequest httpRequest) {
        requireCapacity("bootstrap", clientIp(httpRequest), bootstrapMax);

        if (adminSetupToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Admin bootstrap is not configured. Set ADMIN_SETUP_TOKEN to enable it.");
        }
        boolean tokenMatches = MessageDigest.isEqual(
                adminSetupToken.getBytes(StandardCharsets.UTF_8),
                request.getSetupToken().getBytes(StandardCharsets.UTF_8));
        if (!tokenMatches) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid setup token");
        }
        if (userRepository.countByRole("ADMIN") > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An admin account already exists");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No account exists for " + request.getEmail() + ". Register first, then bootstrap."));
        user.setRole("ADMIN");
        userRepository.save(user);

        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        // Housekeeping: drop expired rows so the table does not grow without bound.
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        RefreshToken stored = new RefreshToken(user, hash(refreshToken),
                LocalDateTime.now().plusDays(refreshTokenTtlDays));
        refreshTokenRepository.save(stored);

        return new AuthResponse(jwtUtil.generateToken(user.getEmail(), user.getRole()), refreshToken);
    }

    private void requireCapacity(String action, String clientKey, int maxRequests) {
        if (!rateLimiter.tryAcquire(action + ":" + clientKey, maxRequests, window)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many " + action + " attempts. Try again shortly.");
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
