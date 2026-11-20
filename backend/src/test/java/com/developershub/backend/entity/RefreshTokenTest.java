package com.developershub.backend.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void isUsableWhileTheTokenIsLive() {
        RefreshToken token = new RefreshToken(new User(), "hash", LocalDateTime.now().plusHours(1));

        assertThat(token.isUsable(LocalDateTime.now())).isTrue();
    }

    @Test
    void isNotUsableOnceRevoked() {
        RefreshToken token = new RefreshToken(new User(), "hash", LocalDateTime.now().plusHours(1));
        token.revoke();

        assertThat(token.isUsable(LocalDateTime.now())).isFalse();
    }

    @Test
    void isNotUsableOnceExpired() {
        RefreshToken token = new RefreshToken(new User(), "hash", LocalDateTime.now().minusHours(1));

        assertThat(token.isUsable(LocalDateTime.now())).isFalse();
    }

    @Test
    void isNotUsableWhenTheExpiryIsExactlyNow() {
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);
        RefreshToken token = new RefreshToken(new User(), "hash", expiry);

        // Expiry is exclusive: a token is dead the instant it expires.
        assertThat(token.isUsable(expiry)).isFalse();
    }

    @Test
    void revokeRecordsTheRevocationTimestamp() {
        RefreshToken token = new RefreshToken(new User(), "hash", LocalDateTime.now().plusHours(1));
        assertThat(token.getRevokedAt()).isNull();

        token.revoke();

        assertThat(token.getRevokedAt()).isNotNull();
        assertThat(token.isUsable(LocalDateTime.now())).isFalse();
    }

    @Test
    void revokingTwiceKeepsTheTokenUnusable() throws InterruptedException {
        RefreshToken token = new RefreshToken(new User(), "hash", LocalDateTime.now().plusHours(1));
        token.revoke();
        LocalDateTime firstRevocation = token.getRevokedAt();

        Thread.sleep(5);
        token.revoke();

        // revoke() stamps the current time again rather than preserving the first
        // stamp, so only the "not before the first revocation" part is guaranteed.
        assertThat(token.getRevokedAt()).isNotNull();
        assertThat(token.getRevokedAt()).isAfterOrEqualTo(firstRevocation);
        assertThat(token.isUsable(LocalDateTime.now())).isFalse();
    }
}
