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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "student@example.com";
    private static final String PASSWORD = "Password123!";
    private static final String ENCODED = "encoded-password";
    private static final String ACCESS_TOKEN = "jwt-access-token";
    private static final String SETUP_TOKEN = "setup-secret";

    @Mock
    UserRepository userRepository;
    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtUtil jwtUtil;
    @Mock
    RateLimiter rateLimiter;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtUtil,
                rateLimiter, 7L, SETUP_TOKEN, 10, 5, 3, 60);
    }

    @Test
    void register_savesUserWithHashedPasswordAndReturnsTokens() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED);
        when(jwtUtil.generateToken(EMAIL, "USER")).thenReturn(ACCESS_TOKEN);

        AuthResponse response = service.register(authRequest(EMAIL, PASSWORD), new MockHttpServletRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getPassword()).isEqualTo(ENCODED);
        assertThat(saved.getPassword()).isNotEqualTo(PASSWORD);
        assertThat(saved.getRole()).isEqualTo("USER");

        assertThat(response.getToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isNotBlank();
    }

    @Test
    void register_persistsOnlyTheHashOfTheRefreshToken() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED);
        when(jwtUtil.generateToken(EMAIL, "USER")).thenReturn(ACCESS_TOKEN);

        AuthResponse response = service.register(authRequest(EMAIL, PASSWORD), new MockHttpServletRequest());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        String storedHash = captor.getValue().getTokenHash();
        assertThat(storedHash).isEqualTo(sha256Hex(response.getRefreshToken()));
        assertThat(storedHash).isNotEqualTo(response.getRefreshToken());
        assertThat(storedHash).hasSize(64);
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(1L, ENCODED)));

        assertThatThrownBy(() -> service.register(authRequest(EMAIL, PASSWORD), new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_rateLimited_throwsTooManyRequests() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.register(authRequest(EMAIL, PASSWORD), new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_returnsTokensForValidCredentials() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(1L, ENCODED)));
        when(passwordEncoder.matches(PASSWORD, ENCODED)).thenReturn(true);
        when(jwtUtil.generateToken(EMAIL, "USER")).thenReturn(ACCESS_TOKEN);

        AuthResponse response = service.login(authRequest(EMAIL, PASSWORD), new MockHttpServletRequest());

        assertThat(response.getToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isNotBlank();
    }

    @Test
    void login_unknownEmail_stillBurnsTheDummyHash() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(authRequest(EMAIL, PASSWORD), new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        // The dummy hash check keeps the response time of an unknown email
        // indistinguishable from a wrong password, so account existence cannot
        // be inferred from the latency.
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).matches(eq(PASSWORD), hashCaptor.capture());
        assertThat(hashCaptor.getValue()).startsWith("$2a$10$").hasSize(60);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(1L, ENCODED)));
        when(passwordEncoder.matches(PASSWORD, ENCODED)).thenReturn(false);

        assertThatThrownBy(() -> service.login(authRequest(EMAIL, PASSWORD), new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_rateLimited_throwsTooManyRequests() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.login(authRequest(EMAIL, PASSWORD), new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void refresh_rotatesTheTokenAndRevokesTheOldOne() {
        String rawToken = "old-refresh-token";
        RefreshToken stored = refreshToken(user(1L, ENCODED), rawToken, LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(stored));
        when(jwtUtil.generateToken(EMAIL, "USER")).thenReturn(ACCESS_TOKEN);

        AuthResponse response = service.refresh(refreshRequest(rawToken));

        assertThat(stored.getRevokedAt()).isNotNull();
        assertThat(response.getToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isNotBlank().isNotEqualTo(rawToken);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(sha256Hex(response.getRefreshToken()));
    }

    @Test
    void refresh_unknownToken_throwsUnauthorized() {
        when(refreshTokenRepository.findByTokenHash(sha256Hex("nope"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(refreshRequest("nope")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_revokedToken_throwsUnauthorized() {
        String rawToken = "revoked-refresh-token";
        RefreshToken stored = refreshToken(user(1L, ENCODED), rawToken, LocalDateTime.now().plusDays(1));
        stored.revoke();
        when(refreshTokenRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.refresh(refreshRequest(rawToken)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_expiredToken_throwsUnauthorized() {
        String rawToken = "expired-refresh-token";
        RefreshToken stored = refreshToken(user(1L, ENCODED), rawToken, LocalDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.refresh(refreshRequest(rawToken)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logout_revokesTheStoredToken() {
        String rawToken = "known-refresh-token";
        RefreshToken stored = refreshToken(user(1L, ENCODED), rawToken, LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(stored));

        service.logout(refreshRequest(rawToken));

        assertThat(stored.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logout_unknownToken_isANoOp() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        service.logout(refreshRequest("unknown"));

        verify(refreshTokenRepository, never()).save(any());
        verify(refreshTokenRepository, never()).deleteByExpiresAtBefore(any());
    }

    @Test
    void bootstrapAdmin_withoutConfiguredSetupToken_throwsServiceUnavailable() {
        AuthService unconfigured = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                jwtUtil, rateLimiter, 7L, "   ", 10, 5, 3, 60);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);

        assertThatThrownBy(() -> unconfigured.bootstrapAdmin(bootstrapRequest(EMAIL, SETUP_TOKEN), new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));

        verify(userRepository, never()).countByRole(anyString());
    }

    @Test
    void bootstrapAdmin_wrongSetupToken_throwsForbidden() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.bootstrapAdmin(bootstrapRequest(EMAIL, "wrong-token"), new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userRepository, never()).countByRole(anyString());
    }

    @Test
    void bootstrapAdmin_existingAdmin_throwsConflict() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(userRepository.countByRole("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> service.bootstrapAdmin(bootstrapRequest(EMAIL, SETUP_TOKEN), new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
    }

    @Test
    void bootstrapAdmin_unregisteredEmail_throwsNotFound() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(userRepository.countByRole("ADMIN")).thenReturn(0L);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.bootstrapAdmin(bootstrapRequest(EMAIL, SETUP_TOKEN), new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(userRepository, never()).save(any());
    }

    @Test
    void bootstrapAdmin_promotesTheUserAndReturnsTokens() {
        User user = user(1L, ENCODED);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(userRepository.countByRole("ADMIN")).thenReturn(0L);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(EMAIL, "ADMIN")).thenReturn(ACCESS_TOKEN);

        AuthResponse response = service.bootstrapAdmin(bootstrapRequest(EMAIL, SETUP_TOKEN), new MockHttpServletRequest());

        assertThat(user.getRole()).isEqualTo("ADMIN");
        verify(userRepository).save(user);
        assertThat(response.getToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isNotBlank();
    }

    @Test
    void register_usesTheFirstForwardedForEntryAsTheRateLimitKey() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED);
        when(jwtUtil.generateToken(EMAIL, "USER")).thenReturn(ACCESS_TOKEN);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        request.setRemoteAddr("198.51.100.7");
        service.register(authRequest(EMAIL, PASSWORD), request);

        verify(rateLimiter).tryAcquire(eq("register:203.0.113.5"), eq(5), eq(Duration.ofSeconds(60)));
    }

    @Test
    void register_fallsBackToTheRemoteAddressAsTheRateLimitKey() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED);
        when(jwtUtil.generateToken(EMAIL, "USER")).thenReturn(ACCESS_TOKEN);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.7");
        service.register(authRequest(EMAIL, PASSWORD), request);

        verify(rateLimiter).tryAcquire(eq("register:198.51.100.7"), eq(5), eq(Duration.ofSeconds(60)));
    }

    @Test
    void register_usesADifferentRateLimitKeyPerClientIp() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED);
        when(jwtUtil.generateToken(EMAIL, "USER")).thenReturn(ACCESS_TOKEN);

        MockHttpServletRequest forwarded = new MockHttpServletRequest();
        forwarded.addHeader("X-Forwarded-For", "203.0.113.5");
        MockHttpServletRequest direct = new MockHttpServletRequest();
        direct.setRemoteAddr("198.51.100.7");

        service.register(authRequest(EMAIL, PASSWORD), forwarded);
        service.register(authRequest(EMAIL, PASSWORD), direct);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimiter, times(2)).tryAcquire(keyCaptor.capture(), anyInt(), any());
        assertThat(keyCaptor.getAllValues()).containsExactly("register:203.0.113.5", "register:198.51.100.7");
    }

    private static AuthRequest authRequest(String email, String password) {
        AuthRequest request = new AuthRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private static RefreshRequest refreshRequest(String rawToken) {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(rawToken);
        return request;
    }

    private static BootstrapRequest bootstrapRequest(String email, String setupToken) {
        BootstrapRequest request = new BootstrapRequest();
        request.setEmail(email);
        request.setSetupToken(setupToken);
        return request;
    }

    private static User user(Long id, String password) {
        User user = new User();
        user.setId(id);
        user.setEmail(EMAIL);
        user.setPassword(password);
        return user;
    }

    private static RefreshToken refreshToken(User user, String rawToken, LocalDateTime expiresAt) {
        return new RefreshToken(user, sha256Hex(rawToken), expiresAt);
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
